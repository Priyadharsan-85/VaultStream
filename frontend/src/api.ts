import type { DemoOverview } from './types';

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL?.replace(/\/$/, '') ?? '';
export const demoOverviewUrl = `${apiBaseUrl}/api/demo/overview`;
export const demoLiveUrl = `${apiBaseUrl}/api/demo/live`;

export async function getDemoOverview(signal?: AbortSignal): Promise<DemoOverview> {
  const response = await fetch(demoOverviewUrl, {
    headers: { Accept: 'application/json' },
    signal,
  });

  if (!response.ok) {
    throw new Error(`Demo API returned ${response.status}. The demo service may be starting or sleeping.`);
  }

  return response.json() as Promise<DemoOverview>;
}
