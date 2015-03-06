<#escape x as x?html>
<meta charset="utf-8">
<html>
<head>
    <title>Preferences Console</title>
    <style>
        body {
            font-family: Consolas, Menlo, Monaco, Lucida Console, Liberation Mono,
                DejaVu Sans Mono, Bitstream Vera Sans Mono, Courier New, monospace, serif;
            text-align: center; background-color: #565656; color: #A9B7C6;
        }

        h1 { background-color: #363636; }

        h1 table { font-size: inherit; text-align: center; }

        a { color: #c99065; text-decoration: none; font-weight: bold; }

        a:hover { color: #ffc66d; text-decoration: underline; }

        a.btn, input[type="button"], input[type="submit"] {
            background-color: #646464; padding: 1% 40%; color: #c99065; font-weight: normal;
            font-size: 100%; border: none; cursor: pointer; cursor: hand; margin: 0;
        }

        a.btn:hover, input[type="button"]:hover, input[type="submit"]:hover {
            background-color: #5a5a5a; color: #ffc66d; text-decoration: none;
        }

        form { display: inline; }

        div.tree { float: left; width: 25%; text-align: center; background-color: #363636; border: 1px solid #aaaaaa; }

        div.keys { float: right; width: 70%; text-align: right; background-color: #323232; border: 1px solid #aaaaaa; }

        div.tree a { font-size: larger; }
        td.child input { font-weight: bold; font-size: larger; }

        input, textarea {
            font-family: Menlo, Consolas, Monaco, Lucida Console, Liberation Mono,
                DejaVu Sans Mono, Bitstream Vera Sans Mono, Courier New, monospace, serif;
            background-color: #2b2b2b; border: solid 1px #cccccc; color: #efefef; font-size: inherit; width: 100%;
        }

        input.add-key { font-weight: bold; text-align: right; }

        table { width: 100%; border-collapse: collapse; table-layout: fixed; }

        table tr td, table tr th { padding: 1%; }

        div.content { overflow: auto; max-width: 100%; }

        table tr th { border-bottom: 1px solid #555555; }

        div.children table tr th { padding: 3%; text-align: left }
        .child { width: 85%; padding: 3%; }
        .add-child { width: 15%; padding: 3%; }

        td.parent {width: 12%; text-align: center; vertical-align: middle; margin: 10%; }
        td.nodepath { width: 74%; text-align: center; }
        td.export, td.rmnode { text-align: center; vertical-align: middle;  margin: 10%; }
        td.export { width: 8%; font-size: 80%; }
        td.rmnode { width: 6%; font-size: 60%; }

        .pref-key { width: 38%; text-align: right; padding: 1%; font-weight: bold; }
        .pref-value { width: 50%; padding: 1%; text-align: left; }
        .savebtn, .delbtn { width: 6%; }

        div.zoom { text-align: center; margin: 8% 10%; }
        div.zoom input[type="submit"] { width: 10%; padding: 1% 3%; background-color: #484848; font-size: xx-large; }
        div.zoom input[type="submit"]:hover { background-color: #404040; }

    </style>

    <script>
        function validateKey(form) {
            if (form.elements['key'].value) {
                form.action = document.location + form.elements['key'].value + '?_method=put';
                return true
            } else {
                alert('Error: empty key!');
                return false
            }
        }
    </script>
</head>
<body>
<h1>
    <table class="top">
        <tr>
            <#assign hasParent = (links?size > 1 && links[1].rel == "parent" && links[1].href?length < links[0].href?length)>
            <td class="parent">
                <#if hasParent>
                    <a class="btn" href="${links[1].href}" title="parent">&#x25b2</a>
                <#else>
                    <a class="btn" title="home" href="..">&#x25E4</a>
                </#if>
            </td>
            <td class="nodepath">
                <#assign nodePath = links[0].href?remove_beginning("/v1")>
                <div class="content">${nodePath}</div>
            </td>
            <#if name??>
            <td class="export">
                <a class="btn" href="${links[0].href}?export=file" title="export">&#x2B07</a>
            </td><td class="rmnode">
                <#if hasParent>
                    <form method="post" action="${links[0].href}?_method=delete"
                          onsubmit="return confirm('Remove ${name}?')">
                        <input type="submit" value="&#x2718" title="remove">
                    </form>
                </#if>
            </td>
            <#else>
            <td colspan="2"></td>
            </#if>
        </tr>
    </table>
    <hr/>
</h1>
<#if nodePath?ends_with("/")>
<div class="tree">
    <div class="children">
        <table>
            <tr>
                <th class="child">Children</th>
                <th class="add-child"></th>
            </tr>
            <#list links as link>
                <#if (link.href?length > links[0].href?length) >
                    <tr>
                        <td class="child">
                            <#assign basename = link.href?replace(links[0].href, "")>
                            <div class="content"><a href="${link.href}">${link.rel}</a></div>
                        </td>
                        <td class="add-child"></td>
                    </tr>
                </#if>
            </#list>
            <tr>
                <form method="post"
                      onsubmit="this.action = document.location + elements['child'].value + '/?_method=put'">
                    <td class="child">
                        <input type="text" name="child" maxlength="80"/>
                    </td>
                    <td class="add-child">
                        <input type="submit" value="&#x2714" title="add"/>
                    </td>
                </form>
            </tr>
        </table>
    </div>
</div>
<div class="keys">
    <table>
        <tr>
            <th class="pref-key">Key</th>
            <th class="pref-value">Value</th>
            <th class="savebtn"></th>
            <th class="delbtn"></th>
        </tr>
        <#list content as pref>
            <tr>
                <form method="post" action="${pref.links[0].href}?_method=put">
                    <td class="pref-key"><div class="content"><a href="${pref.links[0].href}">${pref.key}</a></div></td>
                    <td class="pref-value"><input type="text" name="value"
                                                  onchange="this.form.elements['apply'].value='&#x23ce'" value="${pref.value}"
                                                  maxlength="4096"/></td>
                    <td class="savebtn"><input name="apply" type="submit" value="&#x2713" title="save"/></td>
                </form>
                <form method="post" action="${pref.links[0].href}?_method=delete">
                    <td class="delbtn"><input type="submit" value="&#x2717" title="remove"/></td>
                </form>
            </tr>
        </#list>
        <tr>
            <form method="post" onsubmit="return validateKey(this)">
                <td class="pref-key"><input class="add-key" type="text" name="key" maxlength="80"/></td>
                <td class="pref-value"><input type="text" name="value" maxlength="4096"/></td>
                <td><input type="submit" value="&#x2713" title="add"/></td>
                <td></td>
            </form>
        </tr>
    </table>
</div>
<#else>
<div class="zoom">
<form method="post" action="?_method=put">
    <textarea rows="15" name="value" maxlength="4096">${value}</textarea>
    <br/><br/>
    <input name="apply" type="submit" value="&#x2714" title="save"/>
</form>
</div>
</#if>
</body>
</html>
</#escape>
