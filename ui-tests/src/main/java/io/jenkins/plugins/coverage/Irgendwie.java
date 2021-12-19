package io.jenkins.plugins.coverage;

import java.net.URL;

import org.openqa.selenium.WebElement;

import com.google.inject.Injector;

import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.PageObject;

public class Irgendwie extends PageObject {
    private final String id;

    public Irgendwie(final Build parent, final String id) {
        super(parent,parent.url(id));

        this.id = id;

        //WebElement summary = getElement(By.)


    }
}
