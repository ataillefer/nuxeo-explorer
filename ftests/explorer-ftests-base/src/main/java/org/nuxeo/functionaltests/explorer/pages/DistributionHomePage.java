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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.explorer.pages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.nuxeo.functionaltests.Required;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 11.1
 */
public class DistributionHomePage extends AbstractExplorerPage {

    @Required
    @FindBy(xpath = "//h1")
    public WebElement header;

    @Required
    @FindBy(xpath = "//div[@class='tabscontent']//a[text()='Bundle Groups']")
    public WebElement bundleGroups;

    @Required
    @FindBy(xpath = "//div[@class='tabscontent']//a[text()='Bundles']")
    public WebElement bundles;

    @Required
    @FindBy(xpath = "//div[@class='tabscontent']//a[text()='Components']")
    public WebElement components;

    @Required
    @FindBy(xpath = "//div[@class='tabscontent']//a[text()='Services']")
    public WebElement services;

    @Required
    @FindBy(xpath = "//div[@class='tabscontent']//a[text()='Extension Points']")
    public WebElement extensionPoints;

    @Required
    @FindBy(xpath = "//div[@class='tabscontent']//a[text()='Contributions']")
    public WebElement contributions;

    @Required
    @FindBy(xpath = "//div[@class='tabscontent']//a[text()='Operations']")
    public WebElement operations;

    @Required
    @FindBy(xpath = "//div[@class='tabscontent']//a[text()='Packages']")
    public WebElement packages;

    @Required
    @FindBy(xpath = "//ul[@class='exports']")
    public WebElement exports;

    public DistributionHomePage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void check() {
        checkTitle("Nuxeo Platform Explorer");
        checkHeader(null);
        // check export links presence but do not click (too costly)
        exports.findElement(By.linkText("Json Export"));
        exports.findElement(By.linkText("Json Graph"));
        exports.findElement(By.linkText("DOT Graph"));
    }

    public void checkHeader(String distribId) {
        String text = header.getText();
        if (distribId != null) {
            assertEquals(String.format("Browsing Distribution '%s'", distribId), text);
        } else {
            assertTrue(text, text.startsWith("Browsing Distribution"));
        }
    }

    protected int getChipNumber(WebElement elt) {
        return Integer.valueOf(elt.findElement(By.xpath("following-sibling::*")).getText());
    }

    /** @since 20.1.0 */
    public void checkNumber(int nb, WebElement elt) {
        assertEquals(nb, getChipNumber(elt));
    }

    /** @since 20.1.0 */
    public void checkNumbers(int nbBundleGroups, int nbBundles, int nbComponents, int nbServices, int nbExtensionPoints,
            int nbContributions, int nbOperations, int nbPackages) {
        checkNumber(nbBundleGroups, bundleGroups);
        checkNumber(nbBundles, bundles);
        checkNumber(nbComponents, components);
        checkNumber(nbServices, services);
        checkNumber(nbExtensionPoints, extensionPoints);
        checkNumber(nbContributions, contributions);
        checkNumber(nbOperations, operations);
        checkNumber(nbPackages, packages);
    }

}
