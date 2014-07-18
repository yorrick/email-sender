@import ems.models.SmsDisplay._
@import ems.models.SmsDisplay
@import ems.models.NotSavedInMongo

@()(implicit r: RequestHeader)

$(function() {

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS("@ems.controllers.routes.SmsService.updatesSocket().webSocketURL()")
    var elementTemplate = '@ems.views.html.sms.listElement(SmsDisplay(IdMapping.templateTag, FromMapping.templateTag, ToMapping.templateTag, ContentMapping.templateTag, CreationMapping.templateTag, StatusMapping.templateTag))'

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)

        // do not display ping messages
        if (data.content == "ping") {
            return
        }

        var existingElement = $('#' + data.@IdMapping.jsonName);

        if (existingElement.length > 0) {
            var statusSpan = existingElement.find("[data-status=yes]")
            statusSpan.removeClass();
            statusSpan.addClass(data.@StatusMapping.jsonName)
        } else {
            var replaced = elementTemplate.replace("@IdMapping.templateTag", data.@IdMapping.jsonName);
            var replaced = replaced.replace("@FromMapping.templateTag", data.@FromMapping.jsonName);
            var replaced = replaced.replace("@ToMapping.templateTag", data.@ToMapping.jsonName);
            var replaced = replaced.replace("@ContentMapping.templateTag", data.@ContentMapping.jsonName);
            var replaced = replaced.replace("@CreationMapping.templateTag", data.@CreationMapping.jsonName);
            var replaced = replaced.replace("@StatusMapping.templateTag", data.@StatusMapping.jsonName);

            var smsElement = $(replaced).hide().prependTo('#smsList')

            $(".empty-sms").fadeOut("slow").remove();
            smsElement.fadeIn("slow");
        }

    }


    chatSocket.onmessage = receiveEvent

})