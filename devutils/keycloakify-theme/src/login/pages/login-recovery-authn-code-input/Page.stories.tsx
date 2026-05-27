/**
 * WARNING: Before modifying this file, run the following command:
 * 
 * $ npx keycloakify own --path "login/pages/login-recovery-authn-code-input/Page.stories.tsx"
 * 
 * This file is provided by @keycloakify/login-ui-storybook version 250004.2.0.
 * It was copied into your repository by the postinstall script: `keycloakify sync-extensions`.
 */

/* eslint-disable */

import { createKcPageStory, type Meta, type StoryObj } from "../../mocks/KcPageStory";

const { KcPageStory } = createKcPageStory({
    pageId: "login-recovery-authn-code-input.ftl"
});

const meta = {
    title: "login/login-recovery-authn-code-input.ftl",
    component: KcPageStory
} satisfies Meta<typeof KcPageStory>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};
