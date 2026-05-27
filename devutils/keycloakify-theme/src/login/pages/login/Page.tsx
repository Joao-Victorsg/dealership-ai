/**
 * This file has been claimed for ownership from @keycloakify/login-ui version 250004.7.0.
 * To relinquish ownership and restore this file to its original content, run the following command:
 * 
 * $ npx keycloakify own --path "login/pages/login/Page.tsx" --revert
 */

import { assert } from "tsafe/assert";
import { Template } from "../../components/Template";
import { useKcContext } from "../../KcContext";
import { Info } from "./Info";
import { Form } from "./Form";
import { SocialProviders } from "./SocialProviders";

export function Page() {
    const { kcContext } = useKcContext();
    assert(kcContext.pageId === "login.ftl");

    return (
        <Template
            displayMessage={!kcContext.messagesPerField.existsError("username", "password")}
            headerNode="Welcome back."
            subtitleNode="Sign in to continue browsing, save vehicles, and complete a purchase."
            displayInfo={
                kcContext.realm.password &&
                kcContext.realm.registrationAllowed &&
                !kcContext.registrationDisabled
            }
            infoNode={<Info />}
            socialProvidersNode={
                kcContext.realm.password && kcContext.social !== undefined && <SocialProviders />
            }
        >
            <Form />
        </Template>
    );
}
