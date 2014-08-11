@import ems.models.ForwardingDisplay._
@import ems.models.ForwardingDisplay

@()(implicit r: RequestHeader)

$(function() {

    var r = jsRoutes.ems.controllers.ForwardingController.updatesSocket();
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS(r.webSocketURL())
    var elementTemplate = '@ems.views.html.forwarding.listElement(ForwardingDisplay(IdMapping.templateTag, UserIdMapping.templateTag, FromMapping.templateTag, ToMapping.templateTag, ContentMapping.templateTag, CreationMapping.templateTag, StatusCodeMapping.templateTag, StatusMapping.templateTag, SpinMapping.templateTag, SmsToEmailMapping.templateTag, EmailToSmsMapping.templateTag))'

    // given forwardingData, updates the spinners
    var updateSpinner = function(forwardingData) {
        var existingElement = $('#' + forwardingData.@IdMapping.jsonName);

        // spinning classes
        var statusButton = existingElement.find("button.status");
        var spinner = statusButton.find("i");
        spinner.removeClass();

        if (forwardingData.@SpinMapping.jsonName == "true") {
            spinner.addClass("fa fa-spinner fa-spin");
        }
    }

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)

        // do not display ping messages
        if (data.content == "ping") {
            return
        }

        var existingElement = $('#' + data.@IdMapping.jsonName);

        if (existingElement.length > 0) {
            var statusButton = existingElement.find("button.status");
            var statusSpan = statusButton.find("span");

            // change button text
            statusSpan.text(data.@StatusMapping.jsonName);

            // change button classes
            statusButton.removeClass();
            statusButton.addClass("status btn btn-xs btn-info")
            statusButton.addClass(data.@StatusCodeMapping.jsonName)

        } else {
            var replaced = elementTemplate.replace("@IdMapping.templateTag", data.@IdMapping.jsonName);
            var replaced = replaced.replace("@UserIdMapping.templateTag", data.@UserIdMapping.jsonName);
            var replaced = replaced.replace("@FromMapping.templateTag", data.@FromMapping.jsonName);
            var replaced = replaced.replace("@ToMapping.templateTag", data.@ToMapping.jsonName);
            var replaced = replaced.replace("@ContentMapping.templateTag", data.@ContentMapping.jsonName);
            var replaced = replaced.replace("@CreationMapping.templateTag", data.@CreationMapping.jsonName);
            var replaced = replaced.replace("@StatusCodeMapping.templateTag", data.@StatusCodeMapping.jsonName);
            var replaced = replaced.replace("@StatusMapping.templateTag", data.@StatusMapping.jsonName);
            var replaced = replaced.replace("@SpinMapping.templateTag", data.@SpinMapping.jsonName);

            var forwardingElement = $(replaced).hide().prependTo('#forwardingList')

            $(".empty-forwarding").fadeOut("slow").remove();
            forwardingElement.fadeIn("slow");
        }

        updateSpinner(data);
    }

    chatSocket.onmessage = receiveEvent
})