/**
 * WARNING: Before modifying this file, run the following command:
 * 
 * $ npx keycloakify own --path "login/pages/link-idp-action/Page.stories.tsx"
 * 
 * This file is provided by @keycloakify/login-ui-storybook version 250004.2.0.
 * It was copied into your repository by the postinstall script: `keycloakify sync-extensions`.
 */

/* eslint-disable */

import { createKcPageStory, type Meta, type StoryObj } from "../../mocks/KcPageStory";

const mockKcContext = {
    idpDisplayName: "GitHub",
    url: {
        loginAction: "/login-action"
    }
};

const { KcPageStory } = createKcPageStory({ pageId: "link-idp-action.ftl" });

const meta = {
    title: "login/link-idp-action.ftl",
    component: KcPageStory
} satisfies Meta<typeof KcPageStory>;

export default meta;

type Story = StoryObj<typeof meta>;

/**
 * Default:
 * - Purpose: Tests the default behavior with mock data.
 * - Scenario: Renders with a mocked identity provider name (`GitHub`) and login action URL (`/login-action`).
 * - Key Aspect: Ensures the page renders the prompt and buttons with standard kcContext values.
 */
export const Default: Story = {
    render: () => <KcPageStory kcContext={mockKcContext} />
};

/**
 * WithDifferentProvider:
 * - Purpose: Tests behavior when linking a different provider.
 * - Scenario: Simulates linking to Google with a custom login action URL.
 * - Key Aspect: Verifies dynamic provider name rendering while preserving button actions.
 */
export const WithDifferentProvider: Story = {
    args: {
        kcContext: {
            ...mockKcContext,
            idpDisplayName: "Google",
            url: {
                loginAction: "/custom-login-action"
            }
        }
    }
};
