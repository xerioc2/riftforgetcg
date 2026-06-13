export const APP_VERSION = import.meta.env.VITE_APP_VERSION ?? '0.1.0-alpha';
export const BUILD_TAG = import.meta.env.VITE_BUILD_TAG ?? import.meta.env.VITE_GIT_SHA ?? 'local-dev';
export const BUILD_DATE = import.meta.env.VITE_BUILD_DATE ?? 'local-dev';

export function appBuildLabel() {
  const dateLabel = BUILD_DATE === 'local-dev' ? '' : `, built ${BUILD_DATE}`;
  return `${APP_VERSION} (${BUILD_TAG}${dateLabel})`;
}
