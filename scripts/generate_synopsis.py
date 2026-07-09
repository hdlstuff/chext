#!/usr/bin/env python3
"""Generate docs/synopsis.md from docs/synopsis.txt.

The input format is intentionally small and INI-like:

    [section.some_id]
    kind = heading | paragraph | code | table
    title = ...
    text <<END
    multiline text
    END

    [row.some_table.001]
    construct = ...
    intent <<END
    ...
    END
    details <<END
    ...
    END
    hierarchy <<END
    source links
    END

Rows are attached to the most recent table whose id matches the middle part of
the row section name.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
import html
import re
import sys


ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "docs"
SRC = DOCS / "synopsis.txt"
DST = DOCS / "synopsis.md"


BADGE_STYLES = {
    "Component": ("#e8f1ff", "#174ea6"),
    "Container": ("#eaf7ea", "#137333"),
    "Module": ("#fff4d6", "#8a5a00"),
    "Interface": ("#f3e8ff", "#6b21a8"),
    "Function": ("#ffe8ef", "#a50e38"),
    "UniquePrefix": ("#f1f3f4", "#3c4043"),
    "tpe": ("#fff0e6", "#a14200"),
    "namePrefix": ("#eef0ff", "#3730a3"),
    "Scala": ("#e7f0ff", "#0b4f9c"),
    "SysC": ("#e8f5e9", "#1b5e20"),
    "Scala TB": ("#edf2ff", "#364fc7"),
    "SysC TB": ("#e6fcf5", "#087f5b"),
}


@dataclass
class Block:
    name: str
    fields: dict[str, str] = field(default_factory=dict)


def parse(path: Path) -> list[Block]:
    blocks: list[Block] = []
    current: Block | None = None
    multiline_key: str | None = None
    multiline_end: str | None = None
    multiline_lines: list[str] = []

    for raw in path.read_text().splitlines():
        line = raw.rstrip("\n")

        if multiline_key is not None:
            if line == multiline_end:
                assert current is not None
                current.fields[multiline_key] = "\n".join(multiline_lines).rstrip()
                multiline_key = None
                multiline_end = None
                multiline_lines = []
            else:
                multiline_lines.append(line)
            continue

        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue

        if stripped.startswith("[") and stripped.endswith("]"):
            current = Block(stripped[1:-1])
            blocks.append(current)
            continue

        if current is None:
            raise ValueError(f"field outside section: {line}")

        multi = re.match(r"^([A-Za-z0-9_.-]+)\s*<<([A-Za-z0-9_-]+)$", stripped)
        if multi:
            multiline_key = multi.group(1)
            multiline_end = multi.group(2)
            multiline_lines = []
            continue

        key, sep, value = line.partition("=")
        if not sep:
            raise ValueError(f"expected key=value: {line}")
        current.fields[key.strip()] = value.strip()

    if multiline_key is not None:
        raise ValueError(f"unterminated multiline field {multiline_key}")
    return blocks


def badge(kind: str, text: str | None = None) -> str:
    bg, fg = BADGE_STYLES[kind]
    label = text or kind
    return (
        f'<span style="background:{bg};color:{fg};padding:2px 6px;'
        f'border-radius:4px;">{html.escape(label)}</span>'
    )


def expand_badges(text: str) -> str:
    def repl(match: re.Match[str]) -> str:
        body = match.group(1)
        if ":" in body:
            kind, label = body.split(":", 1)
            return badge(kind, f"{kind}: {label}")
        return badge(body)

    return re.sub(r"\{\{badge:([^}]+)\}\}", repl, text)


def expand_refs(text: str) -> str:
    def repl(match: re.Match[str]) -> str:
        path = match.group(1)
        href = "../" + path
        label = Path(path).name
        return f'<a href="{html.escape(href)}"><code>{html.escape(label)}</code></a>'

    return re.sub(r"\{\{src:([^}]+)\}\}", repl, text)


def source_kind(kind: str, path: str) -> str:
    if path.startswith("src/test/"):
        return "Scala TB"
    if path.startswith("sysc_tb/"):
        return "SysC TB"
    return kind


def source_link(kind: str, path: str) -> str:
    href = "../" + path
    label = source_kind(kind, path)
    return (
        f'<a href="{html.escape(href)}" style="text-decoration:none;">'
        f'{badge(label)}</a>'
    )


def render_source_links(text: str) -> str:
    entries: list[str] = []
    for raw in text.splitlines():
        kind_match = re.search(r"\{\{badge:(Scala|SysC)\}\}", raw)
        if kind_match is None:
            continue
        kind = kind_match.group(1)
        for path in re.findall(r"\{\{src:([^}]+)\}\}", raw):
            entries.append(source_link(kind, path))

    if not entries:
        return ""

    items = "".join(f"<div>{entry}</div>" for entry in entries)
    return f'<div style="margin-top:6px;font-size:0.92em;">{items}</div>'


def expand_inline_code(text: str) -> str:
    return re.sub(
        r"`([^`\n]+)`",
        lambda match: f"<code>{html.escape(match.group(1))}</code>",
        text,
    )


def expand_ellipsis(text: str) -> str:
    return text.replace("...", "&hellip;")


def code_block(code: str, lang: str = "") -> str:
    escaped = html.escape(code)
    cls = f' class="language-{html.escape(lang)}"' if lang else ""
    style = (
        "white-space:pre;overflow-x:auto;margin:6px 0 0;padding:8px;"
        "background:#f6f8fa;border:1px solid #d0d7de;border-radius:6px;"
        "line-height:1.45;"
    )
    return f'<pre style="{style}"><code{cls}>{escaped}</code></pre>'


def render_text(text: str) -> str:
    def repl(match: re.Match[str]) -> str:
        lang = match.group(1) or ""
        return code_block(match.group(2).strip("\n"), lang)

    pieces = re.split(r"(```(?:scala|bash|text)?\n.*?\n```)", text, flags=re.DOTALL)
    rendered: list[str] = []
    for piece in pieces:
        if piece.startswith("```"):
            rendered.append(
                re.sub(r"```(scala|bash|text)?\n(.*?)\n```", repl, piece, flags=re.DOTALL)
            )
        else:
            piece = expand_badges(piece)
            piece = expand_refs(piece)
            piece = expand_inline_code(piece)
            piece = expand_ellipsis(piece)
            rendered.append(piece)
    return "".join(rendered)


def paragraphize(text: str) -> str:
    rendered = render_text(text)
    pieces = re.split(r"(<pre\b.*?</pre>)", rendered, flags=re.DOTALL)
    return "".join(
        piece if piece.startswith("<pre") else piece.replace("\n", "<br>")
        for piece in pieces
    )


def slug(title: str) -> str:
    value = re.sub(r"[^a-z0-9 -]", "", title.lower())
    return re.sub(r"\s+", "-", value).strip("-")


def table_id(block_name: str) -> str:
    return block_name.split(".", 1)[1]


def row_table_id(block_name: str) -> str:
    parts = block_name.split(".")
    if len(parts) < 3 or parts[0] != "row":
        raise ValueError(f"bad row name: {block_name}")
    return parts[1]


def generate(blocks: list[Block]) -> str:
    rows: dict[str, list[Block]] = {}
    for block in blocks:
        if block.name.startswith("row."):
            rows.setdefault(row_table_id(block.name), []).append(block)

    out: list[str] = [
        "<!-- Generated from docs/synopsis.txt by scripts/generate_synopsis.py. Do not edit by hand. -->"
    ]
    toc_items = [
        block.fields["title"]
        for block in blocks
        if block.fields.get("kind") in {"heading", "table"}
    ]
    for block in blocks:
        if block.name.startswith("row."):
            continue

        kind = block.fields.get("kind", "")
        if block.name == "document":
            out.append(f"# {block.fields['title']}")
            if text := block.fields.get("intro", ""):
                out.append("")
                out.append(render_text(text))
            if toc_items:
                out.append("")
                out.append("## Contents")
                out.append("")
                for title in toc_items:
                    out.append(f"- [{title}](#{slug(title)})")
            continue

        if kind == "heading":
            out.append("")
            out.append(f"## {block.fields['title']}")
        elif kind == "paragraph":
            out.append("")
            out.append(render_text(block.fields["text"]))
        elif kind == "code":
            out.append("")
            lang = block.fields.get("lang", "")
            out.append(f"```{lang}")
            out.append(block.fields["text"])
            out.append("```")
        elif kind == "badges":
            out.append("")
            for name in block.fields["items"].split(","):
                name = name.strip()
                out.append(badge(name))
            out.append("")
            out.append(render_text(block.fields["text"]))
        elif kind == "table":
            tid = table_id(block.name)
            out.append("")
            out.append(f"## {block.fields['title']}")
            if preface := block.fields.get("preface", ""):
                out.append("")
                out.append(render_text(preface))
            out.append("")
            out.append("<table>")
            out.append("  <thead>")
            out.append("    <tr>")
            out.append("      <th>Construct</th>")
            out.append("      <th>Intent and Usage</th>")
            out.append("      <th>Details</th>")
            out.append("    </tr>")
            out.append("  </thead>")
            out.append("  <tbody>")
            for row in rows.get(tid, []):
                construct = render_text(row.fields["construct"])
                source_links = render_source_links(row.fields.get("hierarchy", ""))
                if source_links:
                    construct = construct + source_links
                intent = paragraphize(row.fields.get("intent", ""))
                details = paragraphize(row.fields.get("details", row.fields.get("naming", "")))
                out.append("    <tr>")
                out.append(f"      <td>{construct}</td>")
                out.append(f"      <td>{intent}</td>")
                out.append(f"      <td>{details}</td>")
                out.append("    </tr>")
            out.append("  </tbody>")
            out.append("</table>")
        else:
            raise ValueError(f"unknown block kind for [{block.name}]: {kind}")

    return "\n".join(out).rstrip() + "\n"


def main() -> int:
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else SRC
    dst = Path(sys.argv[2]) if len(sys.argv) > 2 else DST
    dst.write_text(generate(parse(src)))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
