package cc.blynk.server.core.model.widgets.controls;

import cc.blynk.server.core.model.DataStream;
import cc.blynk.server.core.model.enums.PinMode;
import cc.blynk.server.core.model.widgets.HardwareSyncWidget;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import static cc.blynk.server.core.protocol.enums.Command.APP_SYNC;
import static cc.blynk.server.core.protocol.enums.Command.HARDWARE;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.utils.StringUtils.prependDashIdAndDeviceId;
import static cc.blynk.utils.StringUtils.BODY_SEPARATOR_STRING;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class RGB extends MultiPinWidget implements HardwareSyncWidget {

    public boolean splitMode;

    public boolean sendOnReleaseOn;

    public int frequency;

    @Override
    public void sendHardSync(ChannelHandlerContext ctx, int msgId, int deviceId) {
        if (dataStreams == null || this.deviceId != deviceId) {
            return;
        }
        if (isSplitMode()) {
            for (DataStream dataStream : dataStreams) {
                if (dataStream.notEmptyAndIsValid()) {
                    ctx.write(makeUTF8StringMessage(HARDWARE, msgId,
                            dataStream.makeHardwareBody()), ctx.voidPromise());
                }
            }
        } else {
            if (dataStreams[0].notEmptyAndIsValid()) {
                ctx.write(makeUTF8StringMessage(HARDWARE, msgId,
                        dataStreams[0].makeHardwareBody()), ctx.voidPromise());
            }
        }
    }

    @Override
    public void sendAppSync(Channel appChannel, int dashId, int targetId) {
        if (dataStreams == null) {
            return;
        }
        if (targetId == ANY_TARGET || this.deviceId == targetId) {
            if (isSplitMode()) {
                for (DataStream dataStream : dataStreams) {
                    if (dataStream.notEmptyAndIsValid()) {
                        String body = prependDashIdAndDeviceId(dashId, deviceId, dataStream.makeHardwareBody());
                        appChannel.write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body),
                                appChannel.voidPromise());
                    }
                }
            } else {
                if (dataStreams[0].notEmptyAndIsValid()) {
                    String body = prependDashIdAndDeviceId(dashId, deviceId, dataStreams[0].makeHardwareBody());
                    appChannel.write(makeUTF8StringMessage(APP_SYNC, SYNC_DEFAULT_MESSAGE_ID, body),
                            appChannel.voidPromise());
                }
            }
        }
    }

    public Integer getRgbValueAsInt() {
        String[] colorsArray = new String[3];

        if (isSplitMode() && dataStreams.length == 3) {
            for (int i = 0; i < 3; i++) {
                if (dataStreams[i].value == null) {
                    colorsArray[i] = "0";
                } else {
                    colorsArray[i] = dataStreams[i].value;
                }
            }
        } else {
            if (dataStreams[0].notEmptyAndIsValid()) {
                String[] pinValues = dataStreams[0].value.split(BODY_SEPARATOR_STRING);
                if (pinValues.length == 3) {
                    colorsArray = pinValues;
                }
            }
        }

        Integer red = getColorFromStringValue(colorsArray[0]);
        Integer green = getColorFromStringValue(colorsArray[1]);
        Integer blue = getColorFromStringValue(colorsArray[2]);

        Integer color = 0;
        color |= red << 16;
        color |= green << 8;
        color |= blue;

        return color;
    }

    public boolean isSplitMode() {
        return splitMode;
    }

    @Override
    public PinMode getModeType() {
        return PinMode.out;
    }

    @Override
    public int getPrice() {
        return 400;
    }

    private Integer getColorFromStringValue(String value) {
        return Integer.parseInt(value) & 0xFF;
    }

}
