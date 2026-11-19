#!/usr/bin/env python3
"""Crea exclusivamente repositorios privados y vacíos tras una aprobación auditada.

El evento procede de issue_comment (nunca ejecuta código proporcionado por la incidencia).
No expone el PAT ni lo imprime.
"""
import json
import os
import re
import sys
from urllib.error import HTTPError
from urllib.request import Request, urlopen

OWNER = "skylan2684"
PREFIX = "[RepoFactory] "
APPROVAL = "AUTORIZAR: PRIVADO-VACIO"
VALID_NAME = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,99}\Z")
API = "https://api.github.com"


def github_api(method, path, token, body=None):
    headers = {
        "Accept": "application/vnd.github+json",
        "Authorization": "Bearer " + token,
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "donpinol-repo-factory",
    }
    data = None
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    request = Request(API + path, data=data, method=method, headers=headers)
    try:
        with urlopen(request, timeout=20) as response:
            return json.load(response)
    except HTTPError as error:
        # Nunca incluir los encabezados de autorización en la salida.
        detail = error.read().decode("utf-8", errors="replace")[:500]
        print(f"GitHub respondió HTTP {error.code}: {detail}", file=sys.stderr)
        raise


def main():
    with open(os.environ["GITHUB_EVENT_PATH"], encoding="utf-8") as source:
        event = json.load(source)

    issue = event.get("issue", {})
    comment = event.get("comment", {})
    repository = event.get("repository", {})
    sender = event.get("sender", {})

    # Solo el propietario puede abrir la solicitud y emitir la aprobación.
    if not (
        event.get("action") == "created"
        and repository.get("full_name") == OWNER + "/DonPinol-Car"
        and repository.get("private") is True
        and issue.get("user", {}).get("login") == OWNER
        and comment.get("user", {}).get("login") == OWNER
        and sender.get("login") == OWNER
        and issue.get("state") == "open"
        and "pull_request" not in issue
    ):
        print("Evento no autorizado o fuera de alcance; no se crea nada.")
        return

    title = issue.get("title", "")
    if not title.startswith(PREFIX) or comment.get("body", "").strip() != APPROVAL:
        print("No es una aprobación exacta de RepoFactory.")
        return

    name = title[len(PREFIX):]
    if not VALID_NAME.fullmatch(name):
        raise ValueError("Nombre de repositorio inválido.")

    token = os.environ.get("REPO_FACTORY_PAT", "")
    if not token:
        raise RuntimeError("Falta el secreto REPO_FACTORY_PAT: no se intentó crear el repositorio.")

    # API restringida: solo cuenta personal, privado, sin inicializar, sin licencia ni gitignore.
    result = github_api(
        "POST", "/user/repos", token,
        {
            "name": name,
            "private": True,
            "auto_init": False,
            "has_issues": True,
            "has_projects": False,
            "has_wiki": False,
        },
    )
    if (
        result.get("full_name", "").lower() != (OWNER + "/" + name).lower()
        or result.get("private") is not True
    ):
        raise RuntimeError("La respuesta no confirmó el propietario y la privacidad esperados.")

    url = result.get("html_url", "https://github.com/" + OWNER + "/" + name)
    print("Repositorio privado creado: " + url)
    issue_token = os.environ.get("ISSUE_COMMENT_TOKEN", "")
    issue_number = issue.get("number")
    if issue_token and isinstance(issue_number, int):
        try:
            github_api(
                "POST",
                f"/repos/{OWNER}/DonPinol-Car/issues/{issue_number}/comments",
                issue_token,
                {"body": "RepoFactory: repositorio **privado y vacío** creado y confirmado: " + url},
            )
            github_api(
                "PATCH",
                f"/repos/{OWNER}/DonPinol-Car/issues/{issue_number}",
                issue_token,
                {"state": "closed", "state_reason": "completed"},
            )
        except HTTPError:
            print("Aviso: repositorio creado; no se pudo registrar/cerrar la incidencia.", file=sys.stderr)


if __name__ == "__main__":
    main()
