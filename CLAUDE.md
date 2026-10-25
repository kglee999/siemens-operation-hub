# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Early stage: a base UI (layout + design sample pages) exists; no domain code, authentication, or persistence layer yet. `/` redirects to `/sample/home` until a real home screen exists. Base package: `com.operation.hub`.

## Stack

- Java 17, Maven (use the wrapper: `./mvnw` in Bash, `.\mvnw.cmd` in PowerShell)
- Spring Boot **4.1.2**. Boot 4 uses split, module-specific starters (`spring-boot-starter-webmvc`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`). Don't assume Boot 2/3 starter names, package locations, or auto-config classes. Check the Boot 4 docs when unsure.
- Spring Security (`spring-boot-starter-security`, `-security-test`, `thymeleaf-extras-springsecurity6`) is currently commented out in `pom.xml`, so every URL is open. The sample login screen and top bar are static and use no `sec:` attributes.
- Lombok is wired as an annotation processor in `maven-compiler-plugin` for both main and test compilation.
- Packaged as a **WAR**: `ServletInitializer` supports deployment to an external servlet container, and `spring-boot-starter-tomcat` is `provided`. The built WAR still runs standalone with `java -jar`.

## Commands

```bash
./mvnw spring-boot:run                 # run locally (DevTools enabled for auto-restart)
./mvnw clean package                   # build WAR into target/
./mvnw test                            # run all tests
./mvnw test -Dtest=SiemensOperationHubApplicationTests                    # single test class
```

No linter or formatter is configured.

## Frontend (Thymeleaf + Bootstrap 5)

- **Design target: CS Webtool**, the sibling project `../gb-siemens` (JSP + Bootstrap 3 + Gentelella). `static/assets/css/style.css` was tuned so rendered sizes, colors and fonts match CS Webtool, measured element by element in a headless browser.
  - Class names match it: `left_col`, `top_nav`, `right_col`, `page-title`, `x_panel`/`x_title`/`x_content`, `jambo_table`, `my_menu`, `user-profile`, `si-*` icons.
  - When changing styles, compare against CS Webtool instead of falling back to Bootstrap 5 defaults.
  - Don't copy gb-siemens's `style.css` or Bootstrap 3 markup (`col-xs-*`, `pull-right`, `panel`, jQuery plugins) back in.
- Key values taken from CS Webtool:
  - Body: 13px, line-height 1.471, color `#333`, letter-spacing -0.3px. Links `#5a738e`.
  - Inputs: 14px, 34px tall, border `#ccc`, no rounded corners.
  - Buttons: 14px, 34px tall, 3px corners (sm 30px, lg 46px). `.btn-default` is the Bootstrap 3 white button.
  - Search labels: bold.
  - Radios and checkboxes: the iCheck `green.png` sprite applied to `.form-check-input`.
  - Top bar: 73px. Table cells: 8px padding, light-grey header.
  - Modal: Bootstrap 3 look (white header, footer buttons centered).
  - Validation: only invalid fields are marked (red), with no green check on valid ones.
- Theme mechanics: colors and sizes are CSS variables on `:root` (`--oh-*`). Bootstrap is themed by overriding its `--bs-*` and component variables there, not with Sass.
- Font: same stack as CS Webtool (`--oh-font-family`: "Helvetica Neue", Roboto, Arial, "Droid Sans", sans-serif).
  - English and digits render in Arial; Korean renders in the PC's system font. CS Webtool declares a Noto web font but a later Gentelella `body` rule overrides it.
  - The `Noto Sans CJK KR Web` files in `static/assets/font/` are declared but unused, as in CS Webtool.
  - To force one Korean font everywhere, add that name to `--oh-font-family`. The files only cover the 2,350 common Hangul syllables (KS X 1001); rarer ones and symbols like ①※㈜ fall back to the system font.
- Vendored libraries (static files, no CDN/WebJars), under `static/assets/vendor/<lib>-<version>/`:
  - Bootstrap **5.3.8** and Bootstrap Icons **1.13.2**.
  - DataTables **3.1.4** (MIT; v3 works without jQuery).
  - FullCalendar **7.1.3** (MIT, vanilla; the global bundle works without native Temporal).
    - Use `assets/js/oh-calendar.js` → `oh.calendar(el, opts)`, which applies Korean, the CS Webtool toolbar, and the `#CALENDAR` colours.
    - Its resource timeline is a paid premium feature. The Real-time 모니터링 timeline is plain HTML/CSS instead.
  - Handsontable **6.2.2**: the same version CS Webtool uses, and the last MIT release. 7.0+ needs a commercial licence, so do not upgrade.
    - Use `assets/js/oh-handsontable.js` → `oh.grid(el, opts)`.
    - Empty rows go yellow via `cells: oh.grid.emptyRowCells()`. Read-only cells get red text.
    - Only for spreadsheet-style editing screens. Ordinary lists use plain tables.
  - To upgrade, replace the folder and update the paths that reference it (`grep -r vendor/<lib>` in `templates/`).
- No jQuery. `static/assets/js/common.js` holds the sidebar/active-menu logic and `window.oh` helpers that replace gb-siemens's `modal_show` / `$.ajax` / `confirm` / `alert` / loading bar:
  - `oh.modal.open(url, params)` loads a server-rendered `.modal` fragment into `#modal_area`, runs `<script>`s inside it, shows it, and removes it on close.
  - `oh.ajax.postForm(url, form)`, `oh.ajax.post(url, obj)` (urlencoded) and `oh.ajax.postJson(url, data)` return JSON. They send the CSRF header if the meta tags exist.
  - `oh.ajax.getHtml(url, params)` fetches an HTML fragment.
  - `oh.confirm(msg, {okText})` returns a Promise<boolean>.
  - `oh.toast(msg, type)` shows a toast.
  - `oh.loading.show()` / `oh.loading.hide()` toggle the loading overlay.
  - It also auto-initialises tooltips, `.layer-wrap` click popovers and `.table-freeze` header offsets.
- DataTables pages load `dataTables.min.js`, `dataTables.bootstrap5.min.js`, then `assets/js/oh-datatables.js`, which sets the shared defaults: Korean messages, layout, and 34px controls.
  - Pages call `new DataTable('#id', {...})` with only page-specific options.
  - The `#DATATABLES` section of `style.css` makes it look like `jambo_table`.
- **Thymeleaf gotcha:** `[[...]]` and `[(...)]` inside `<script>` are parsed as inline expressions, which breaks rendering (for example DataTables `order: [[0, 'asc']]`). Use `<script th:inline="none">` for such scripts, or `th:inline="javascript"` when you do want inlining.
- Layout: every page uses `layout:decorate="~{layout/default}"` (thymeleaf-layout-dialect) and fills `layout:fragment="content"` and optionally `script`.
  - Page-specific CSS goes as a plain `<link>` in the page's `<head>`, which gets merged after `style.css`. There is no `css` fragment slot.
  - The sidebar menu lives in `templates/fragments/sidebar.html`. `common.js` marks the active item by matching `location.pathname` against link `href`s, so add menu links there with `th:href`.
- `sample/login.html` and `error.html` are standalone pages (no layout). Use `th:action` on POST forms so a CSRF token is added automatically once security is enabled.
- Design/publishing test screens all live in package `com.operation.hub.sample` (`controller.SampleController`; `dto.SampleUser`, `dto.SampleDutyStore`), with URLs under `/sample/**` and templates in `templates/sample/`:
  - `home`, `login`, `list`, `form`, `components`
  - `table`: plain server-rendered tables with no library: basic list, checkbox selection, a merged header with frozen header and first column plus a totals row, and an empty-list row. This is the default for lists.
    - `.table-freeze` replaces gb-siemens's freeze-table plugin with CSS `position: sticky`. `common.js` sets the second header row's offset.
  - `datatable`, `handsontable`
  - `modal`, which uses `/modal/form`, `/modal/save` and `/modal/rows`. Its data is in-memory and resets on restart.
  - `duty` (CS Webtool "724 당직 관리 (DI)"), with modals `duty-add` and `duty-auto`. It is a working sample backed by in-memory `SampleDutyStore`.
  - Put new test screens here too. There are no automated render tests for sample pages (only `SiemensOperationHubApplicationTests` context load).
- **CS Webtool screen publishing** (static mock-ups of every CS Webtool screen, with dummy data and no backend):
  - Templates live in `templates/sample/cswt/<module>/`. `SampleController` serves them with no per-page Java:
    - `<page>.html` is served at `/sample/cswt/<module>/<page>`.
    - `_<name>.html` (root `th:fragment="modal"`) is served at `/sample/cswt/<module>/modal/<name>`.
    - Names must be lowercase, digits and `-`. Missing templates return 404.
  - Modules mirror gb-siemens view folders: `noti`, `egn`, `mng`, `service-car`, `adm`, `com`, `test`. The sidebar groups (724 Call / 엔지니어 / 기준정보 / 운행기록부 / Admin) follow gb-siemens `menu_side.jsp`.
  - Page-only CSS goes in `static/assets/css/cswt/<group>.css`. Shared CS Webtool patterns are in the `#CSWT COMMON` section of `style.css`, for example:
    - text and notices: `.guide-text`, `.alert-line`, `.text-red`, `.text-blue`, `.section-title`
    - colours: `.legend`, the `bg-*` team/type colours, `.cell-warning`, `.cell-delay`, `.row-total`
    - controls: `.layer-wrap`, `.input-time`, `.time-select`, `.btn-stack`, `.duty-chip`
    - modals and output: `.scroll-box`, `.upload-result`, `.form-static`, `.well`, `.ln_solid`
    - Bootstrap 5's own `bg-warning` clashes with CS Webtool's, so warning cells use `cell-*`.
  - `sample/components` catalogues every CS Webtool UI element and names the screen that uses each one.
- Form controls share fixed heights set by `--oh-input-height` (34px, plus `-sm` 30px and `-lg` 46px) so inputs, selects, date fields and `input-group-text` line up. Use `form-control-lg` and the like instead of per-page height or padding overrides.
- Static/template edits under `src/main/resources` reach a running `spring-boot:run` only after they're copied to `target/classes` (an IDE build or `./mvnw resources:resources`).
