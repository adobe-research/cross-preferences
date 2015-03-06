<#escape x as x?html>
<meta charset="utf-8">
<html>
<head>
    <title>Preferences Console</title>
    <style>
        body {
            font-family: Consolas, Menlo, Monaco, Lucida Console, Liberation Mono,
                DejaVu Sans Mono, Bitstream Vera Sans Mono, Courier New, monospace, serif;
            text-align: center; background-color: #323232; color: #A9B7C6;
        }

        hr { margin-bottom: 8% }

        a {
            color: #c4732e; background-color: #545454; padding: 1% 5%;
            margin: 10%; text-decoration: none; text-transform: uppercase;
        }

        a:hover { text-decoration: underline; color: #f39b2e; background-color: #484848; }

        input {
            font-family: Menlo, Consolas, Monaco, Lucida Console, Liberation Mono,
                DejaVu Sans Mono, Bitstream Vera Sans Mono, Courier New, monospace, serif;
        }

        fieldset { margin: 8% 24%; padding: 5% }

        legend { text-align: left; }
    </style>
</head>
<body>

    <h1>${content}</h1>
    <hr/>
    <h2>
        <a href="${links[0].href}">${links[0].rel}</a>
        <a href="${links[1].href}">${links[1].rel}</a>
    </h2>

    <p>
    <form method="post" enctype="multipart/form-data">
        <fieldset>
            <legend>Upload preferences XML</legend>
            <input type="file" name="file" accept="application/xml,text/xml"/>
            <input type="submit" value="Import"/>
        </fieldset>
    </form>
    </p>

</body>
</html>
</#escape>
