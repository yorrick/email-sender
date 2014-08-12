@import ems.models.ForwardingDisplay._
@import ems.models.ForwardingDisplay

@()(implicit r: RequestHeader)

$(function() {

    var r = jsRoutes.ems.controllers.ForwardingController.updatesSocket();
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS(r.webSocketURL())
    var elementTemplate = '@ems.views.html.forwarding.listElement(ForwardingDisplay(IdMapping.templateTag, UserIdMapping.templateTag, FromMapping.templateTag, ToMapping.templateTag, ContentMapping.templateTag, CreationMapping.templateTag, StatusCodeMapping.templateTag, StatusMapping.templateTag, SmsToEmailMapping.templateTag, EmailToSmsMapping.templateTag))'

    // given forwarding <a> tag and received data, updates the spinners
    var updateSpinner = function(element, data) {
        // spinning classes
        var statusButton = element.find("button.status");
        var spinner = statusButton.find("i");
        spinner.removeClass();

        if (data.@StatusCodeMapping.jsonName == "Sending") {
            spinner.addClass("fa fa-spinner fa-spin");
        }
    }

    // given forwarding <a> tag and received data, update the glyphicon
    var updateGlyphicon = function(element, data) {
          var iconSpan = element.find("span.glyphicon");

          if (data.@SmsToEmailMapping.jsonName == "true") {
              iconSpan.addClass("glyphicon-phone");
          }
          if (data.@EmailToSmsMapping.jsonName == "true") {
              iconSpan.addClass("glyphicon-envelope");
          }
    }

    // given forwarding <a> tag and received data, update the grid offset
    var updateGridClasses = function(element, data) {
          if (data.@EmailToSmsMapping.jsonName == "true") {
              element.addClass("col-md-offset-1");
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
            var replaced = replaced.replace("@SmsToEmailMapping.templateTag", data.@SmsToEmailMapping.jsonName);
            var replaced = replaced.replace("@EmailToSmsMapping.templateTag", data.@EmailToSmsMapping.jsonName);

            var forwardingElement = $(replaced).hide().prependTo('#forwardingList')

            $(".empty-forwarding").fadeOut("slow").remove();
            forwardingElement.fadeIn("slow");
        }

        var element = $('#' + data.@IdMapping.jsonName);

        updateSpinner(element, data);
        updateGlyphicon(element, data);
        updateGridClasses(element, data);
    }

    chatSocket.onmessage = receiveEvent
})