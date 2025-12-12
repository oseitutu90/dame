package com.dame;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@PWA(name = "Dame", shortName = "Dame")
@Theme("dame")
@Push
public class AppShell implements AppShellConfigurator {
}