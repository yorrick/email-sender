@import SmsDisplay._

@()(implicit r: RequestHeader)

$(function() {

    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
    var chatSocket = new WS("@routes.SmsService.updatesSocket().webSocketURL()")
    var elementTemplate = '@views.html.sms.listElement(SmsDisplay(FromMapping.templateTag, ToMapping.templateTag, ContentMapping.templateTag, CreationMapping.templateTag))'

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data)

        console.log(data)

        var smsElement = $(elementTemplate).html(function(index, html){
            var replaced = html.replace("@FromMapping.templateTag", data.@FromMapping.jsonName);
            var replaced = replaced.replace("@ToMapping.templateTag", data.@ToMapping.jsonName);
            var replaced = replaced.replace("@ContentMapping.templateTag", data.@ContentMapping.jsonName);
            var replaced = replaced.replace("@CreationMapping.templateTag", data.@CreationMapping.jsonName);

            return replaced;
        }).hide().prependTo('#smsList')

        $(".empty-sms").fadeOut("slow").remove();
        smsElement.fadeIn("slow");
    }


    chatSocket.onmessage = receiveEvent

})