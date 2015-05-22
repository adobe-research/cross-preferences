<#escape x as x?html>
<meta charset="utf-8">
<html>
<head>
    <title>Preferences Console</title>
    <link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css">
    <link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap-theme.min.css">
    <link rel="stylesheet" href="//cdnjs.cloudflare.com/ajax/libs/x-editable/1.5.1/bootstrap3-editable/css/bootstrap-editable.css"/>
    <style>
        a.clickable-tab {
            color: #337ab7 !important;
        }

        a.clickable-tab:hover {
            cursor: pointer !important;
            color: #439be9 !important;
        }

        .float-right {
            display: inline-block;
            float: right;
        }

        .pref-key {
            width: 40%;
            white-space: nowrap;
        }

        .pref-button {
            text-align: right;
            width: 5%;
        }

        #remove-child .modal-header {
            font-size: smaller;
        }

        #remove-child .modal-body {
            font-size: small;
        }

    </style>
    <script src="//code.jquery.com/jquery-2.1.4.min.js"></script>
    <script src="//maxcdn.bootstrapcdn.com/bootstrap/3.3.4/js/bootstrap.min.js"></script>
    <script src="//cdnjs.cloudflare.com/ajax/libs/x-editable/1.5.1/bootstrap3-editable/js/bootstrap-editable.min.js"></script>

    <script>
        $.fn.editable.defaults.ajaxOptions = {type: "PUT"};
        $.fn.editable.defaults.send = "always";

        $(function () {
            $('#add-child').submit(function (ev) {
                if (ev.target.elements['child'].value) {
                    ev.target.action = document.location + ev.target.elements['child'].value + '/?_method=put';
                    return true;
                } else {
                    $('#no-input').modal('show');
                    return false;
                }
            });
            $('#save-pref').click(function () {
                $('#change-pref').submit();
            });
            $('#change-pref').submit(function (ev) {
                if (ev.target.elements['key'].value) {
                    ev.target.action = document.location + ev.target.elements['key'].value + '?_method=put';
                    if ($('#new-value').hasClass('editable-unsaved')) {
                        ev.target.elements['value'].value = $('#new-value').text();
                    }
                    return true;
                } else {
                    $('#no-input').modal('show');
                    return false;
                }
            });
            $('.editable-value').editable({
                title: 'Edit value',
                rows: 5,
                columns: 80
            });
        });
    </script>
</head>
<body>
    <#assign isSystemNode = links[0].href?starts_with('/sys')/>
    <#assign rootPath = isSystemNode?string('/sys', '/usr')>
<div class="container">
    <#assign segments = links[0].href?remove_beginning(rootPath)?remove_ending('/')?split('/')>
    <form class="navbar-form navbar-right float-right" role="search" action="/" method="post"
          enctype="multipart/form-data">
        <div class="input-group" role="group">
            <span class="input-group-addon">
                <span class="glyphicon glyphicon-import"></span>
            </span>
            <input type="file" class="form-control" name="file" accept="application/xml,text/xml"/>
            <span class="input-group-btn">
                <button type="submit" class="btn btn-default">Import</button>
            </span>
        </div>
    </form>
    <h3 class="row" role="navigation">
        <#assign rootclass= (segments?size == 1)?string('', 'clickable-tab')>
        <ul class="nav nav-tabs">
            <li title="System Preferences" role="presentation" class="${isSystemNode?string('active', '')}">
                <a class="${isSystemNode?string(rootclass, '')}" href="/sys/"><span
                        class="glyphicon glyphicon-cloud"></a>
            </li>
            <li title="User Preferences" role="presentation" class="${isSystemNode?string('', 'active')}">
                <a class="${isSystemNode?string('', rootclass)}" href="/usr/"><span
                        class="glyphicon glyphicon-user"></a>
            </li>
        </ul>
        <ol class="breadcrumb">
            <#if segments?size == 1>
                <li class="active"></li>
            </#if>
            <#list segments as item>
                <#if item_has_next>
                    <li><a href="${rootPath}${segments[0..item_index]?join('/')}/">${item}</a></li>
                <#else>
                    <li class="active">${item}</li>
                    <#assign current_node = item>
                </#if>
            </#list>
            <form action="${links[0].href}" class="float-right">
                <#if current_node?length gt 0>
                    <button type="button" class="btn btn-default" data-toggle="modal" data-target="#remove-child" title="Remove">
                        <span class="glyphicon glyphicon-remove"></span>
                    </button>
                </#if>
                <input type="hidden" name="export" value="file"/>
                <button type="submit" class="btn btn-default" title="Export">
                    <span class="glyphicon glyphicon-export"></span>
                </button>
            </form>
            <div class="modal fade" id="remove-child" tabindex="-1" role="dialog"
                 aria-labelledby="myModalLabel" aria-hidden="true">
                <div class="modal-dialog">
                    <div class="modal-content">
                        <div class="modal-header">
                            Remove ${current_node} ?
                        </div>
                        <div class="modal-body">
                            This will remove the current node and all its descendants!
                        </div>
                        <div class="modal-footer">
                            <form action="${links[0].href}?_method=delete" method="post">
                                <button type="button" class="btn btn-default" data-dismiss="modal">Cancel
                                </button>
                                <button type="submit" class="btn btn-danger btn-ok">Remove</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </ol>
    </h3>
    <div class="row">
        <div class="col-sm-3">
            <div class="panel panel-default">
            <#--<#if links?size gt 1 && links[1].href != rootPath + '/'><#assign extra = 1><#else><#assign extra = 0></#if>-->
            <#--<h3>${links?size} - ${segments?size} - ${extra}</h3>-->
                <div class="panel-heading">Children</div>
                <div class="panel-body">
                    <div class="list-group">
                        <#list links as link>
                            <#if (link.href?length gt links[0].href?length)>
                                <#assign basename = link.href?replace(links[0].href, "")>
                                <a class="list-group-item" href="${link.href}"><strong>${link.rel}</strong></a>
                            </#if>
                        </#list>
                    </div>
                    <form id="add-child" method="post">
                        <div class="input-group" role="group">
                            <span class="input-group-addon">
                                <span class="glyphicon glyphicon-plus"></span>
                            </span>
                            <input name="child" type="text" class="form-control" placeholder="Child name..."/>
                        </div>
                    </form>
                </div>
            </div>
        </div>
        <div class="col-sm-9">
            <div class="panel panel-default">
                <div class="panel-heading">Preferences</div>
                <div class="modal fade" id="no-input" tabindex="-1" role="dialog" aria-labelledby="myModalLabel"
                     aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <div class="modal-header">
                                <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span
                                        aria-hidden="true">&times;</span></button>
                                <h4 class="modal-title" id="myModalLabel">Invalid input</h4>
                            </div>
                            <div class="modal-body">
                                Empty keys / child names don't make much sense, do they?
                            </div>
                            <div class="modal-footer">
                                <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="panel-body">
                    <table class="table">
                        <#list content as pref>
                            <tr class="pref-key">
                                <td>${pref.key}</td>
                                <td class="pref-value">
                                    <a href="#" class="editable-value" data-type="textarea" data-name="value"
                                       data-url="${pref.links[0].href}?_method=put">${pref.value}</a>
                                </td>
                                <td class="pref-button">
                                    <form action="${pref.links[0].href}?_method=delete" method="post">
                                        <button type="submit" class="btn btn-default" title="Remove ${pref.key}">
                                            <span class="glyphicon glyphicon-minus"></span>
                                        </button>
                                    </form>
                                </td>
                            </tr>
                        </#list>
                        <tr>
                            <td class="pref-key">
                                <form id="change-pref" method="post" style="display: inline;">
                                    <div class="input-group">
                                        <span class="input-group-addon">
                                            <span class="glyphicon glyphicon-plus"></span>
                                        </span>
                                        <input type="text" class="form-control" name="key" maxlength="80"
                                               placeholder="Key..."/>
                                    </div>
                                    <input type="hidden" name="value"/>
                                </form>
                            </td>
                            <td class="pref-value">
                                <a href="#" id="new-value" class="editable-value" data-type="textarea"
                                   data-name="value"></a>
                            </td>
                            <td class="pref-button">
                                <button id="save-pref" type="submit" class="btn btn-default">
                                    <span class="glyphicon glyphicon-ok"></span>
                                </button>
                            </td>
                        </tr>
                    </table>
                </div>
            </div>
        </div>

    </div>
</div>
</body>
</html>
</#escape>
