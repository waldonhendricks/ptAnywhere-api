<!DOCTYPE html>
<html>
    <head lang="en">
        <meta charset="UTF-8">
        <title>Console</title>
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body { font: 10pt monospace, Helvetica, Arial; }
            #messages { margin: 0; padding: 0; white-space: pre; }
            #lastLine { float: left; margin-right: 4px; }
            #current { display: block; height: 100%; }
        </style>
        <script src="../../../widget/jquery/jquery.js"></script>
        <script>
             var ws = null;

            function setConnected(connected) {
                $('#current').prop( "disabled", !connected);
            }

            function scrollToBottom() {
                document.location.replace("#bottom");
                // Another alternative registering "redirection" in the browser history.
                // window.location.href = "#bottom"
            }

            function connect(target) {
                console.log('Connecting to websocket endpoint... (' + target + ')');

                if ('WebSocket' in window) {
                    ws = new WebSocket(target);
                } else if ('MozWebSocket' in window) {
                    ws = new MozWebSocket(target);
                } else {
                    alert('WebSocket is not supported by this browser.');
                    return;
                }
                ws.onopen = function () {
                    setConnected(true);
                    console.log('Info: WebSocket connection opened.');
                };
                ws.onmessage = function (event) {
                    var lines = event.data.split("\n");
                    if (lines.length>1) {
                        for (var i=0; i<lines.length-1; i++) { // Unnecessary
                            if (i==0) {
                                var lastLine = $("#lastLine").text();
                                if (lastLine.trim()!=="--More--")
                                    $("#messages").append(lastLine);
                                $("#lastLine").text('');
                            }
                            $("#messages").append(lines[i] + "<br />");
                        }
                    }
                    $("#lastLine").append(lines[lines.length-1]);
                    scrollToBottom();
                    $("#current").focus();
                };
                ws.onerror = function (event) {
                    setConnected(false);
                    console.log('Info: WebSocket error, Code: ' + event.code + (event.reason == "" ? "" : ", Reason: " + event.reason));
                };
                ws.onclose = function (event) {
                    setConnected(false);
                    console.log('Info: WebSocket connection closed, Code: ' + event.code + (event.reason == "" ? "" : ", Reason: " + event.reason));
                };
            }

            $(document).keypress(function(e) {
                if(e.which == 13) {
                    ws.send($("#current").text());
                    $("#current").text('');
                }
            });

            $(function() {
                //connect( ('ws://'+ window.location.host + window.location.pathname).replace("api", "endpoint") );
                connect( '${websocketURL}' );
            });
        </script>
    </head>
    <body>
        <div id="messages"></div>
        <div style="height: 8pt; width: 100%">
            <span id="lastLine"></span>
            <span id="current" contentEditable="true">ping 10.0.0.2</span>
        </div>
        <a name="bottom"></a>
    </body>
</html>