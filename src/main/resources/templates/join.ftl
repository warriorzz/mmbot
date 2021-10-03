<html lang="de">
    <head>
        <title>Join</title>
    </head>
    <body style="background-color: dimgrey">
        <h1>Please enter your discord id!</h1>
        <div style="margin-left: 5%; display: flow-root; text-align: center">
            <form method="get" action="joinparty">
                <div style="margin-left: 1%">
                    <label>
                        <textarea name="dcid" maxlength="18" minlength="18"></textarea>
                    </label>
                </div>
                <div style="margin-left: 1%">
                    <label>
                        <textarea name="token" id="token" style="display: none">${options.token}</textarea>
                    </label>
                </div>
                <div style="margin-left: 1%">
                    <label>
                        <textarea name="state" id="state" style="display: none">${options.state}</textarea>
                    </label>
                </div>
                <div style="margin-left: 1%">
                    <button id="submit">Submit</button>
                </div>
            </form>
        </div>
    </body>
</html>