/*
 *    Copyright 2025 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package team.idealstate.minecraft.next.common.uuid;

import java.util.UUID;
import team.idealstate.minecraft.next.common.validate.Validation;
import team.idealstate.minecraft.next.common.validate.annotation.NotNull;

public abstract class UUIDUtils {

    private static final int BIN_LEN = 16;
    private static final int BIN_SUFFIX = 8;
    private static final int[] SWAP = {6, 4, 0, 2};
    private static final int[] RESTORE = {4, 6, 2, 0};

    public static UUID binaryToUUID(byte[] binary) {
        return binaryToUUID(binary, false);
    }

    public static UUID binaryToUUID(@NotNull byte[] binary, boolean swapFlag) {
        Validation.notNull(binary, "binary must not be null");
        Validation.is(binary.length == BIN_LEN, "binary's length must be equal to " + BIN_LEN);
        if (swapFlag) {
            byte[] copiedBinary = new byte[BIN_LEN];
            for (int i = 0, j = 0; j < SWAP.length; j++) {
                int k = SWAP[j];
                copiedBinary[i++] = binary[k++];
                copiedBinary[i++] = binary[k];
            }
            System.arraycopy(binary, BIN_SUFFIX, copiedBinary, BIN_SUFFIX, BIN_LEN - BIN_SUFFIX);
            binary = copiedBinary;
        }
        long mostBit =
                ((((long) binary[0] & 0xFF) << 56)
                        | (((long) binary[1] & 0xFF) << 48)
                        | (((long) binary[2] & 0xFF) << 40)
                        | (((long) binary[3] & 0xFF) << 32)
                        | (((long) binary[4] & 0xFF) << 24)
                        | (((long) binary[5] & 0xFF) << 16)
                        | (((long) binary[6] & 0xFF) << 8)
                        | (((long) binary[7] & 0xFF)));
        long leastBit =
                ((((long) binary[8] & 0xFF) << 56)
                        | (((long) binary[9] & 0xFF) << 48)
                        | (((long) binary[10] & 0xFF) << 40)
                        | (((long) binary[11] & 0xFF) << 32)
                        | (((long) binary[12] & 0xFF) << 24)
                        | (((long) binary[13] & 0xFF) << 16)
                        | (((long) binary[14] & 0xFF) << 8)
                        | (((long) binary[15] & 0xFF)));
        return new UUID(mostBit, leastBit);
    }

    public static byte[] uuidToBinary(@NotNull String uuid) {
        return uuidToBinary(UUID.fromString(uuid), false);
    }

    public static byte[] uuidToBinary(@NotNull String uuid, boolean swapFlag) {
        return uuidToBinary(UUID.fromString(uuid), swapFlag);
    }

    public static byte[] uuidToBinary(@NotNull UUID uuid) {
        return uuidToBinary(uuid, false);
    }

    public static byte[] uuidToBinary(@NotNull UUID uuid, boolean swapFlag) {
        Validation.notNull(uuid, "uuid must not be null");
        long mostBit = uuid.getMostSignificantBits();
        byte[] binary = new byte[16];
        binary[0] = (byte) ((mostBit >> 56) & 0xFF);
        binary[1] = (byte) ((mostBit >> 48) & 0xFF);
        binary[2] = (byte) ((mostBit >> 40) & 0xFF);
        binary[3] = (byte) ((mostBit >> 32) & 0xFF);
        binary[4] = (byte) ((mostBit >> 24) & 0xFF);
        binary[5] = (byte) ((mostBit >> 16) & 0xFF);
        binary[6] = (byte) ((mostBit >> 8) & 0xFF);
        binary[7] = (byte) (mostBit & 0xFF);
        long leastBit = uuid.getLeastSignificantBits();
        binary[8] = (byte) ((leastBit >> 56) & 0xFF);
        binary[9] = (byte) ((leastBit >> 48) & 0xFF);
        binary[10] = (byte) ((leastBit >> 40) & 0xFF);
        binary[11] = (byte) ((leastBit >> 32) & 0xFF);
        binary[12] = (byte) ((leastBit >> 24) & 0xFF);
        binary[13] = (byte) ((leastBit >> 16) & 0xFF);
        binary[14] = (byte) ((leastBit >> 8) & 0xFF);
        binary[15] = (byte) (leastBit & 0xFF);
        if (swapFlag) {
            for (int i = 0, j = 0; j < RESTORE.length; j++) {
                int k = RESTORE[j];
                byte b = binary[i];
                binary[i++] = binary[k];
                binary[k++] = b;
                b = binary[i];
                binary[i++] = binary[k];
                binary[k] = b;
            }
        }
        return binary;
    }
}
