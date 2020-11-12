/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *     Kevin Leturc <kleturc@nuxeo.com>
 *     Anahide Tchertchian
 */

/**
* Script handling automated export of explorer-generated artifacts.
*
* This script will build a docker image with target packages to be snapshotted.
* The snashot will then be exported and uploaded to a target explorer instance.
*/
properties([
  [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/ataillefer/nuxeo-explorer/'],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '10', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

// all packages released with the platform
def defaultPackages = 'cas2-authentication easyshare nuxeo-csv nuxeo-drive nuxeo-imap-connector nuxeo-multi-tenant nuxeo-platform-importer nuxeo-quota nuxeo-signature nuxeo-template-rendering shibboleth-authentication nuxeo-liveconnect nuxeo-platform-3d'
// additional packades released independently, usually after a platform release promotion
def additionalPackages = 'nuxeo-diff nuxeo-platform-user-registration nuxeo-virtualnavigation nuxeo-web-ui nuxeo-jsf-ui nuxeo-showcase-content nuxeo-spreadsheet'

String getExportImageVersion(String explorerVersion, String nuxeoVersion) {
  def ev = explorerVersion.trim()
  def nv = nuxeoVersion.trim()
  if (ev.isEmpty()) {
    ev = 'unknown';
  }
  return "${ev}-${nv}"
}

String getExplorerPackageId(String version) {
  def res = "platform-explorer"
  if (!version.isEmpty()) {
    res += '-' + version.trim()
  }
  return res
}

String getPackageList(downloadPackagesFromProd, defaultPackages, additionalPackages) {
  def res = "${defaultPackages} ${downloadPackagesFromProd ? additionalPackages : ''}".trim()
  if (res.isEmpty()) {
    // avoid variable not to be declared
    return " "
  }
  return res
}

String getClid(clid) {
  // replace lines by "--"
  return sh(returnStdout: true, script: """#!/bin/bash +x
    echo -e \"${clid}\" | sed ':a;N;\$!ba;s/\\n/--/g'
  """)
}

pipeline {

  agent {
    label 'jenkins-nuxeo-package-11'
  }

  parameters {
    // build params
    string(name: 'NUXEO_VERSION', defaultValue: '', description: 'Mandatory Version of the target Nuxeo Server Image.\nSample: \'11.3.48\' or \'11.3\'.')
    string(name: 'NUXEO_EXPLORER_VERSION', defaultValue: '', description: 'Optional Version of the Explorer package to be used (should be compatible with Nuxeo version).\nSample: \'20.0.0-RC2\' or \'20.0.0\'.')
    booleanParam(name: 'DOWNLOAD_PACKAGES_FROM_PROD', defaultValue: false, description: 'Download packages from production (otherwise, preprod will be used)')
    text(name: 'DEFAULT_PACKAGE_LIST', defaultValue: defaultPackages, description: 'The list of packages to install for snapshot.\nSample: \'nuxeo-csv nuxeo-quota-1.0.0\'.')
    text(name: 'ADDITIONAL_PACKAGE_LIST', defaultValue: additionalPackages, description: 'The additional list of packages to install for snapshot.\nThis list will only be taken into account when DOWNLOAD_PACKAGES_FROM_PROD option is checked (promoted Nuxeo version use case).')
    // export params
    string(name: 'SNAPSHOT_NAME', defaultValue: 'Nuxeo Platform', description: 'Name of the distribution that will be exported.')
    booleanParam(name: 'PERFORM_JSON_EXPORT', defaultValue: false, description: 'Perform download of json export on top of zip export (content will be archived).')
    // upload params
    booleanParam(name: 'UPLOAD_EXPORT', defaultValue: true, description: 'Upload export to target Explorer site (snapshot will be archived anyway).')
    booleanParam(name: 'UPLOAD_TO_PROD', defaultValue: false, description: 'Upload export to production Explorer site (otherwise, preview will be used).')
    booleanParam(name: 'UPLOAD_AS_PROMOTED', defaultValue: false, description: 'Upload export as a promoted version (this will impact aliases used for uploaded snapshot).')
    string(name: 'UPLOAD_ALIASES', defaultValue: '', description: 'Additional aliases to setup on the uploaded snapshot.\nSample: \'firstAlias\nsecondAlias\'.')
  }

  environment {
    NUXEO_DOCKER_REGISTRY = 'docker-private.packages.nuxeo.com'
    NUXEO_IMAGE_VERSION = "${params.NUXEO_VERSION}"
    PREVIEW_NAMESPACE = "explorer-export"

    CONNECT_PROD_URL = 'https://connect.nuxeo.com/nuxeo/site/'
    CONNECT_PREPROD_URL = 'https://nos-preprod-connect.nuxeocloud.com/nuxeo/site/'

    CONNECT_EXPLORER_URL = "${CONNECT_PROD_URL}"
    NUXEO_EXPLORER_PACKAGE = getExplorerPackageId(params.NUXEO_EXPLORER_VERSION)

    CONNECT_EXPORT_URL = "${params.DOWNLOAD_PACKAGES_FROM_PROD ? CONNECT_PROD_URL : CONNECT_PREPROD_URL}"
    EXPORT_PACKAGE_LIST = getPackageList(params.DOWNLOAD_PACKAGES_FROM_PROD, params.DEFAULT_PACKAGE_LIST, params.ADDITIONAL_PACKAGE_LIST)

    VERSION = getExportImageVersion(params.NUXEO_EXPLORER_VERSION, NUXEO_IMAGE_VERSION)
    // NXP-29494: override templates to avoid activating s3
    NUXEO_TEMPLATE_OVERRIDE = 'nuxeo.templates=default'

    UPLOAD_EXPORT = "${params.UPLOAD_EXPORT}"
    UPLOAD_URL = "${params.UPLOAD_TO_PROD ? 'https://explorer.nuxeo.com/nuxeo' : 'https://preview-nuxeo-explorer-master.platform.dev.nuxeo.com/nuxeo'}"
    UPLOAD_CREDS_ID = "${params.UPLOAD_TO_PROD ? 'explorer-prod' : 'explorer-preview'}"

    CURL_OPTIONS = "--fail --location --connect-timeout 180 --max-time 300 --retry 2"
    SLACK_CHANNEL = 'explorer-notifs'
  }

  stages {

    stage('Check Parameters') {
      steps {
        script {
          echo """
          ----------------------------------------
          Nuxeo version:                 '${NUXEO_IMAGE_VERSION}'
          Explorer package:              '${NUXEO_EXPLORER_PACKAGE}'

          Connect Explorer URL:          '${CONNECT_EXPLORER_URL}'
          Connect Export URL:            '${CONNECT_EXPORT_URL}'
          Download Packages from Prod:   '${params.DOWNLOAD_PACKAGES_FROM_PROD}'
          Package List:                  '${EXPORT_PACKAGE_LIST}'
          Snapshot Name:                 '${params.SNAPSHOT_NAME}'
          Perform Json Export:           '${params.PERFORM_JSON_EXPORT}'

          Upload Export:                 '${UPLOAD_EXPORT}'
          Upload URL:                    '${UPLOAD_URL}'
          Upload as Promoted:            '${params.UPLOAD_AS_PROMOTED}'
          ----------------------------------------
          """
          if (params.NUXEO_VERSION.isEmpty()) {
            currentBuild.result = 'ABORTED';
            def message = 'NUXEO_VERSION parameters is mandatory'
            currentBuild.description = "${message}"
            echo "${message}"
            error(currentBuild.description)
          }
          currentBuild.description = "Generating reference export for ${NUXEO_IMAGE_VERSION}"
        }
      }
    }

    // stage('Set Labels') {
    //   steps {
    //     container('maven') {
    //       echo """
    //       ----------------------------------------
    //       Set Kubernetes resource labels
    //       ----------------------------------------
    //       """
    //       echo "Set label 'branch: export' on pod ${NODE_NAME}"
    //       sh """
    //         kubectl label pods ${NODE_NAME} branch=export
    //       """
    //     }
    //   }
    // }

    stage('Build Docker Image') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Build Explorer Export Docker Image
          ----------------------------------------
          Image tag: ${VERSION}
          Registry: ${DOCKER_REGISTRY}
          """
          withCredentials([string(credentialsId: 'instance-clid', variable: 'INSTANCE_CLID')]) {
            script {
              def moduleDir = 'docker/nuxeo-explorer-export-docker'
              def oneLineClid = getClid("${INSTANCE_CLID}")
              sh """
                echo "oneLineClid=${oneLineClid}"
                cat ${moduleDir}/skaffold.yaml
                CONNECT_EXPLORER_CLID="${oneLineClid}" envsubst < ${moduleDir}/skaffold.yaml > ${moduleDir}/skaffold.yaml~gen
                cat ${moduleDir}/skaffold.yaml~gen
              """
              // sh """
              //   skaffold build -f ${moduleDir}/skaffold.yaml~gen
              // """
            }
          }
        }
      }
    }
  }

}
