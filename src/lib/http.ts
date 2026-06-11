const STATUS_LABELS: Record<number, string> = {
  400: 'Invalid request',
  403: 'Session expired or unauthorized',
  404: 'Not found',
  409: 'Conflict',
};

export async function readableHttpError(response: Response, fallback = 'Request failed.'): Promise<string> {
  const body = await response.text().catch(() => '');
  const status = STATUS_LABELS[response.status] ?? `Server returned ${response.status}`;
  return body.trim() ? `${status}: ${body.trim()}` : `${status}: ${fallback}`;
}

export async function fetchJsonOrThrow<T>(url: string, init?: RequestInit, fallback?: string): Promise<T> {
  const response = await fetch(url, init);
  if (!response.ok) throw new Error(await readableHttpError(response, fallback));
  return response.json() as Promise<T>;
}
