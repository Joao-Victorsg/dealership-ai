/**
 * This file has been claimed for ownership from @keycloakify/login-ui version 250004.7.0.
 * To relinquish ownership and restore this file to its original content, run the following command:
 * 
 * $ npx keycloakify own --path "login/components/Template/Template.tsx" --revert
 */

import type { ReactNode } from "react";
import { useEffect } from "react";
import { clsx } from "@keycloakify/login-ui/tools/clsx";
import { kcSanitize } from "@keycloakify/login-ui/kcSanitize";
import { useSetClassName } from "@keycloakify/login-ui/tools/useSetClassName";
import { useInitializeTemplate } from "./useInitializeTemplate";
import { useKcClsx } from "@keycloakify/login-ui/useKcClsx";
import { useI18n } from "../../i18n";
import { useKcContext } from "../../KcContext";

function getOrigin(rawUrl: string | undefined): string | undefined {
    if (rawUrl === undefined || rawUrl === "") {
        return undefined;
    }

    try {
        const parsed = new URL(rawUrl);
        return `${parsed.protocol}//${parsed.host}`;
    } catch {
        return undefined;
    }
}

export function Template(props: {
    displayInfo?: boolean;
    displayMessage?: boolean;
    displayRequiredFields?: boolean;
    headerNode: ReactNode;
    subtitleNode?: ReactNode;
    socialProvidersNode?: ReactNode;
    infoNode?: ReactNode;
    documentTitle?: string;
    bodyClassName?: string;
    children: ReactNode;
}) {
    const {
        displayInfo = false,
        displayMessage = true,
        displayRequiredFields = false,
        headerNode,
        subtitleNode = null,
        socialProvidersNode = null,
        infoNode = null,
        documentTitle,
        bodyClassName,
        children
    } = props;

    const { kcContext } = useKcContext();

    const { msg, msgStr, currentLanguage, enabledLanguages } = useI18n();

    const { kcClsx } = useKcClsx();

    const isFrontendOrigin = (origin: string | undefined): origin is string =>
        origin !== undefined &&
        (
            origin.includes("app.localhost") ||
            origin.includes("localhost:3000") ||
            origin.includes("127.0.0.1:3000")
        );

    const clientBaseOrigin = getOrigin(kcContext.client?.baseUrl);
    const trustedClientBaseOrigin = isFrontendOrigin(clientBaseOrigin)
        ? clientBaseOrigin
        : undefined;
    const redirectUriOrigin =
        typeof window === "undefined"
            ? undefined
            : getOrigin(new URLSearchParams(window.location.search).get("redirect_uri") ?? undefined);
    const trustedRedirectUriOrigin = isFrontendOrigin(redirectUriOrigin)
        ? redirectUriOrigin
        : undefined;
    const frontendBaseUrl =
        trustedRedirectUriOrigin ??
        trustedClientBaseOrigin ??
        "https://app.localhost:4443";
    const isRegisterPage = kcContext.pageId === "register.ftl";

    useEffect(() => {
        document.title =
            documentTitle ?? msgStr("loginTitle", kcContext.realm.displayName || kcContext.realm.name);
    }, []);

    useSetClassName({
        qualifiedName: "html",
        className: kcClsx("kcHtmlClass")
    });

    useSetClassName({
        qualifiedName: "body",
        className: bodyClassName ?? kcClsx("kcBodyClass")
    });

    const { isReadyToRender } = useInitializeTemplate();

    if (!isReadyToRender) {
        return null;
    }

    return (
        <div className={clsx(kcClsx("kcLoginClass"), "aurelio-auth-root")}>
            <header className="aurelio-site-header" aria-label="Aurelio main navigation">
                <div className="aurelio-site-header-inner">
                    <a href={frontendBaseUrl} className="aurelio-site-brand">
                        <span className="aurelio-site-brand-icon">A</span>
                        <span className="aurelio-site-brand-wordmark">
                            <strong>Aurelio</strong> MOTORS
                        </span>
                    </a>
                    <nav className="aurelio-site-nav">
                        <a href={`${frontendBaseUrl.replace(/\/$/, "")}/inventory`}>Inventory</a>
                        <a href={`${frontendBaseUrl.replace(/\/$/, "")}/inventory?condition=NEW`}>New</a>
                        <a href={`${frontendBaseUrl.replace(/\/$/, "")}/inventory?condition=USED`}>Used</a>
                        <a href={`${frontendBaseUrl.replace(/\/$/, "")}/about`}>About</a>
                    </nav>
                    <div className="aurelio-site-auth-links">
                        <a
                            href={kcContext.url.loginUrl}
                            className={clsx("aurelio-site-signin-link", !isRegisterPage && "is-active")}
                        >
                            Sign in
                        </a>
                        {kcContext.realm.registrationAllowed && !kcContext.registrationDisabled && (
                            <a
                                href={kcContext.url.registrationUrl}
                                className={clsx(
                                    "aurelio-site-create-link",
                                    isRegisterPage && "is-active"
                                )}
                            >
                                Create account
                            </a>
                        )}
                    </div>
                </div>
            </header>

            <main className="aurelio-auth-shell-wrap">
                <div className="aurelio-auth-shell">
                    <aside className="aurelio-auth-hero grain" aria-hidden="true">
                        <div>
                            <span className="aurelio-badge">
                                <span className="aurelio-badge-dot" /> Aurelio Motors
                            </span>
                            <h2 className="aurelio-hero-title">A dealership built around you.</h2>
                            <p className="aurelio-hero-copy">
                                Browse without limits. Save what you love. Complete the purchase online
                                — your invoice is delivered straight to your inbox.
                            </p>
                        </div>
                        <ul className="aurelio-hero-highlights">
                            <li>&middot; Verified vehicle histories</li>
                            <li>&middot; Transparent pricing — no surprise fees</li>
                            <li>&middot; White-glove delivery available</li>
                        </ul>
                    </aside>

                    <section className="aurelio-auth-main reveal reveal-1">
                    <div className="aurelio-auth-topbar">
                        <div
                            id="kc-header"
                            className={clsx(kcClsx("kcHeaderWrapperClass"), "aurelio-header-wrapper")}
                        >
                            {kcContext.realm.displayName ?? "Aurelio Motors"}
                        </div>

                        {enabledLanguages.length > 1 && (
                            <div
                                className={clsx(kcClsx("kcLocaleMainClass"), "aurelio-locale")}
                                id="kc-locale"
                            >
                                <div id="kc-locale-wrapper" className={kcClsx("kcLocaleWrapperClass")}>
                                    <div
                                        id="kc-locale-dropdown"
                                        className={clsx("menu-button-links", kcClsx("kcLocaleDropDownClass"))}
                                    >
                                        <button
                                            tabIndex={1}
                                            id="kc-current-locale-link"
                                            aria-label={msgStr("languages")}
                                            aria-haspopup="true"
                                            aria-expanded="false"
                                            aria-controls="language-switch1"
                                        >
                                            {currentLanguage.label}
                                        </button>
                                        <ul
                                            role="menu"
                                            tabIndex={-1}
                                            aria-labelledby="kc-current-locale-link"
                                            aria-activedescendant=""
                                            id="language-switch1"
                                            className={kcClsx("kcLocaleListClass")}
                                        >
                                            {enabledLanguages.map(({ languageTag, label, href }, i) => (
                                                <li
                                                    key={languageTag}
                                                    className={kcClsx("kcLocaleListItemClass")}
                                                    role="none"
                                                >
                                                    <a
                                                        role="menuitem"
                                                        id={`language-${i + 1}`}
                                                        className={kcClsx("kcLocaleItemClass")}
                                                        href={href}
                                                    >
                                                        {label}
                                                    </a>
                                                </li>
                                            ))}
                                        </ul>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>

                    <div id="kc-content" className={clsx(kcClsx("kcFormCardClass"), "aurelio-auth-content")}>
                        <header className={clsx(kcClsx("kcFormHeaderClass"), "aurelio-form-header")}>
                            {(() => {
                                const node = !(
                                    kcContext.auth !== undefined &&
                                    kcContext.auth.showUsername &&
                                    !kcContext.auth.showResetCredentials
                                ) ? (
                                    <h1 id="kc-page-title">{headerNode}</h1>
                                ) : (
                                    <div id="kc-username" className={kcClsx("kcFormGroupClass")}>
                                        <label id="kc-attempted-username">
                                            {kcContext.auth.attemptedUsername}
                                        </label>
                                        <a
                                            id="reset-login"
                                            href={kcContext.url.loginRestartFlowUrl}
                                            aria-label={msgStr("restartLoginTooltip")}
                                        >
                                            <div className="kc-login-tooltip">
                                                <i className={kcClsx("kcResetFlowIcon")}></i>
                                                <span className="kc-tooltip-text">
                                                    {msg("restartLoginTooltip")}
                                                </span>
                                            </div>
                                        </a>
                                    </div>
                                );

                                if (!displayRequiredFields) {
                                    return node;
                                }

                                return (
                                    <div className={kcClsx("kcContentWrapperClass")}>
                                        <div className={clsx(kcClsx("kcLabelWrapperClass"), "subtitle")}>
                                            <span className="subtitle">
                                                <span className="required">*</span>
                                                {msg("requiredFields")}
                                            </span>
                                        </div>
                                        <div className="col-md-10">{node}</div>
                                    </div>
                                );
                            })()}
                            {subtitleNode !== null && (
                                <p className="aurelio-auth-subtitle">{subtitleNode}</p>
                            )}
                        </header>
                        <div id="kc-content-wrapper">
                            {/* App-initiated actions should not see warning messages about the need to complete the action during login. */}
                            {displayMessage &&
                                kcContext.message !== undefined &&
                                (kcContext.message.type !== "warning" ||
                                    !kcContext.isAppInitiatedAction) && (
                                    <div
                                        className={clsx(
                                            `alert-${kcContext.message.type}`,
                                            kcClsx("kcAlertClass"),
                                            `pf-m-${kcContext.message?.type === "error" ? "danger" : kcContext.message.type}`
                                        )}
                                    >
                                        <div className="pf-c-alert__icon">
                                            {kcContext.message.type === "success" && (
                                                <span className={kcClsx("kcFeedbackSuccessIcon")}></span>
                                            )}
                                            {kcContext.message.type === "warning" && (
                                                <span className={kcClsx("kcFeedbackWarningIcon")}></span>
                                            )}
                                            {kcContext.message.type === "error" && (
                                                <span className={kcClsx("kcFeedbackErrorIcon")}></span>
                                            )}
                                            {kcContext.message.type === "info" && (
                                                <span className={kcClsx("kcFeedbackInfoIcon")}></span>
                                            )}
                                        </div>
                                        <span
                                            className={kcClsx("kcAlertTitleClass")}
                                            dangerouslySetInnerHTML={{
                                                __html: kcSanitize(kcContext.message.summary)
                                            }}
                                        />
                                    </div>
                            )}
                            {children}
                            {kcContext.auth !== undefined && kcContext.auth.showTryAnotherWayLink && (
                                <form
                                    id="kc-select-try-another-way-form"
                                    action={kcContext.url.loginAction}
                                    method="post"
                                >
                                    <div className={kcClsx("kcFormGroupClass")}>
                                        <input type="hidden" name="tryAnotherWay" value="on" />
                                        <a
                                            href="#"
                                            id="try-another-way"
                                            onClick={event => {
                                                event.preventDefault();
                                                document.forms[
                                                    "kc-select-try-another-way-form" as never
                                                ].requestSubmit();

                                                return false;
                                            }}
                                        >
                                            {msg("doTryAnotherWay")}
                                        </a>
                                    </div>
                                </form>
                            )}
                            {socialProvidersNode}
                            {displayInfo && (
                                <div id="kc-info" className={kcClsx("kcSignUpClass")}>
                                    <div id="kc-info-wrapper" className={kcClsx("kcInfoAreaWrapperClass")}>
                                        {infoNode}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                    </section>
                </div>
            </main>
        </div>
    );
}
