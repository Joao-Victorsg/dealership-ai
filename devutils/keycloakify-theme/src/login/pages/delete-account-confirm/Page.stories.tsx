/**
 * WARNING: Before modifying this file, run the following command:
 * 
 * $ npx keycloakify own --path "login/pages/delete-account-confirm/Page.stories.tsx"
 * 
 * This file is provided by @keycloakify/login-ui-storybook version 250004.2.0.
 * It was copied into your repository by the postinstall script: `keycloakify sync-extensions`.
 */

/* eslint-disable */

import { createKcPageStory, type Meta, type StoryObj } from "../../mocks/KcPageStory";

const { KcPageStory } = createKcPageStory({ pageId: "delete-account-confirm.ftl" });

const meta = {
    title: "login/delete-account-confirm.ftl",
    component: KcPageStory
} satisfies Meta<typeof KcPageStory>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};
export const WithAIAFlow: Story = {
    args: {
        kcContext: {
            triggered_from_aia: true,
            url: { loginAction: "/login-action" }
        }
    }
};
export const WithoutAIAFlow: Story = {
    args: {
        kcContext: {
            triggered_from_aia: false,
            url: { loginAction: "/login-action" }
        }
    }
};
export const WithCustomButtonStyle: Story = {
    args: {
        kcContext: {
            triggered_from_aia: true,
            url: { loginAction: "/login-action" }
        }
    }
};
