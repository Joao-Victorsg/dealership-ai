/**
 * This file has been claimed for ownership from @keycloakify/login-ui version 250004.7.0.
 * To relinquish ownership and restore this file to its original content, run the following command:
 * 
 * $ npx keycloakify own --path "login/pages/register/Page.tsx" --revert
 */

import { assert } from "tsafe/assert";
import { useKcContext } from "../../KcContext";
import { Template } from "../../components/Template";
import { Form } from "./Form";

export function Page() {
    const { kcContext } = useKcContext();
    assert(kcContext.pageId === "register.ftl");

    return (
        <Template
            headerNode="Create your account."
            subtitleNode="Two short steps. You can change profile details after sign-in."
            displayMessage={kcContext.messagesPerField.exists("global")}
        >
            <ol className="aurelio-register-stepper" aria-label="Registration progress">
                <li className="is-active">
                    <span className="aurelio-register-step-index">1</span>
                    <span>Account</span>
                </li>
                <li className="aurelio-register-step-divider" aria-hidden="true" />
                <li>
                    <span className="aurelio-register-step-index">2</span>
                    <span>Profile</span>
                </li>
            </ol>
            <Form />
        </Template>
    );
}
