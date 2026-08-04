"""Source 2 — Google Maps (interface graphique) via Playwright.

Gratuit, sans clé API, mais : sélecteurs fragiles + ton IP exposée.
Installation : pip install playwright && playwright install chromium
"""

import asyncio
import random
import re
import unicodedata

from playwright.async_api import async_playwright

from . import db, verify


async def scrape(query, location, max_results, conn):
    inserted = 0
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=True)
        page = await browser.new_page(
            locale="fr-FR", viewport={"width": 1280, "height": 900}
        )
        await page.goto("https://www.google.com/maps")
        await page.wait_for_timeout(2500)
        await _accept_consent(page)

        box = page.locator("input#searchboxinput, input[name='q']")
        await box.click()
        await box.fill(f"{query} {location}")
        await box.press("Enter")
        await page.wait_for_timeout(5000)

        handles = []
        for _ in range(max_results // 10 + 2):
            handles = await page.query_selector_all(
                'a[aria-label]:not([aria-label=""])'
            )
            if len(handles) >= max_results:
                break
            await page.mouse.wheel(0, 2500)
            await page.wait_for_timeout(1500)

        seen = set()
        for handle in handles[: max_results]:
            try:
                await handle.click()
                await page.wait_for_timeout(2500)
                name = await _text(page, "h1.DUwDvf")
                if not name or name in seen:
                    continue
                seen.add(name)
                category = _clean_addr(await _text(page, "button.DkEaL"))
                address = _clean_addr(await _text(page, 'button[data-item-id="address"]'))
                phone = await _text(page, 'button[data-item-id^="phone:tel"]')
                lat, lng = _coords_from_url(page.url)

                row = {
                    "id": f"maps_{abs(hash(name))}",
                    "source": "maps",
                    "name": name,
                    "category": category,
                    "address": address,
                    "phone": verify.normalize_phone(phone),
                    "lat": lat,
                    "lng": lng,
                }
                row.update(verify.classify_place(row))
                db.upsert_place(conn, row)
                inserted += 1
                if inserted >= max_results:
                    break
                await page.wait_for_timeout(random.randint(1500, 3000))
            except Exception:
                continue
        await browser.close()
    return inserted


async def _accept_consent(page):
    for selector in (
        "button[aria-label*='Accepter']",
        "button[aria-label*='Accept']",
        "button:has-text('Tout accepter')",
        "button:has-text('Accept all')",
    ):
        try:
            btn = page.locator(selector).first
            if await btn.count():
                await btn.click()
                await page.wait_for_timeout(1500)
                return
        except Exception:
            pass


def _coords_from_url(url):
    m = re.search(r"@(-?\d+\.\d+),(-?\d+\.\d+)", url or "")
    if m:
        return float(m.group(1)), float(m.group(2))
    return None, None


def _clean_addr(s):
    """Enlève icônes Google Maps (zone privée), contrôles, et aplatit."""
    if not s:
        return ""
    s = "".join(
        c for c in s
        if unicodedata.category(c) != "Cc" and not (0xE000 <= ord(c) <= 0xF8FF)
    )
    return re.sub(r"\s+", " ", s).strip()


async def _text(page, selector):
    try:
        loc = page.locator(selector).first
        if await loc.count():
            return (await loc.inner_text()).strip()
    except Exception:
        pass
    return ""
