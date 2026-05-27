/**
 * WARNING: Before modifying this file, run the following command:
 * 
 * $ npx keycloakify own --path "login/mocks/KcPageStory.tsx"
 * 
 * This file is provided by @keycloakify/login-ui-storybook version 250004.2.0.
 * It was copied into your repository by the postinstall script: `keycloakify sync-extensions`.
 */

/* eslint-disable */

// NOTE: This is not a file that you typically want to modify.

import type { DeepPartial } from "@keycloakify/login-ui/tools/DeepPartial";
import { getKcContextMock } from "./getKcContextMock";
import type { KcContext } from "../KcContext";
import KcPage from "../KcPage";
export type { Meta, StoryObj } from "../../kc.gen";

export function createKcPageStory<PageId extends KcContext["pageId"]>(params: { pageId: PageId }) {
    const { pageId } = params;

    function KcPageStory(props: { kcContext?: DeepPartial<Extract<KcContext, { pageId: PageId }>> }) {
        const { kcContext: overrides } = props;

        const kcContextMock = getKcContextMock({
            pageId,
            overrides
        });

        return <KcPage kcContext={kcContextMock} />;
    }

    return { KcPageStory };
}
