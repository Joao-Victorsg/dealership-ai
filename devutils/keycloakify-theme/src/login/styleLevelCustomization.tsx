/**
 * This file has been claimed for ownership from @keycloakify/login-ui version 250004.7.0.
 * To relinquish ownership and restore this file to its original content, run the following command:
 * 
 * $ npx keycloakify own --path "login/styleLevelCustomization.tsx" --revert
 */

import type { ReactNode } from "react";
import type { ClassKey } from "@keycloakify/login-ui/useKcClsx";
import stylesheetUrl from "./aurelio-keycloakify.css?url";

type Classes = { [key in ClassKey]?: string };

type StyleLevelCustomization = {
    doUseDefaultCss: boolean;
    classes?: Classes;
    loadCustomStylesheet?: () => void;
    Provider?: (props: { children: ReactNode }) => ReactNode;
};

export function useStyleLevelCustomization(): StyleLevelCustomization {
    const loadCustomStylesheet = () => {
        if (typeof document === "undefined") {
            return;
        }

        if (document.querySelector("link[data-aurelio-keycloakify-theme='true']")) {
            return;
        }

        const link = document.createElement("link");
        link.rel = "stylesheet";
        link.href = stylesheetUrl;
        link.dataset.aurelioKeycloakifyTheme = "true";
        document.head.appendChild(link);
    };

    return {
        doUseDefaultCss: false,
        loadCustomStylesheet
    };
}
