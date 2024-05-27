package my_protocol;

import framework.IMACProtocol;
import framework.MediumState;
import framework.TransmissionInfo;
import framework.TransmissionType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static framework.MediumState.*;

/**
 * A fairly trivial Medium Access Control scheme.
 *
 * @author Jaco ter Braak, University of Twente
 * @version 05-12-2013
 *
 * Copyright University of Twente,  2013-2024
 *
 **************************************************************************
 *                          = Copyright notice =                          *
 *                                                                        *
 *            This file may ONLY  be distributed UNMODIFIED!              *
 * In particular, a correct solution to the challenge must  NOT be posted *
 * in public places, to preserve the learning effect for future students. *
 **************************************************************************
 */

public class MyProtocol implements IMACProtocol {

    int claimedNumbers;
    int number = -1;
    int latestSuccessInformation;
    int interruptCooldown;
    @Override
    public TransmissionInfo TimeslotAvailable(MediumState previousMediumState,
                                              int controlInformation, int localQueueLength) {
        if (controlInformation != 0 ){
            latestSuccessInformation = controlInformation;

        }
        if(interruptCooldown > 0){
            interruptCooldown = interruptCooldown -1;
        }
        // No data to send, just be quiet
        if (localQueueLength == 0) {
            System.out.println("SLOT - No data to send.");
            if((controlInformation & 0b111) == number){
                int nextQueue = (controlInformation & 0x000fffff) >>> 3;
                if(claimedNumbers > ((controlInformation & 0xfff00000) >>> 20)) {
                    return new TransmissionInfo(TransmissionType.NoData,
                                                (claimedNumbers << 20 | nextQueue));
                }
                return new TransmissionInfo(TransmissionType.NoData,
                                            ((controlInformation & 0xfff00000) | nextQueue));
            }
            if((controlInformation & 0xfff00000 >>> 20) > claimedNumbers) {
                claimedNumbers = (controlInformation & 0xfff00000) >>> 20;
            }
            return new TransmissionInfo(TransmissionType.Silent, 0);
        }

        if(number == -1 ) {
            System.out.println("This user needs to claim a number");
            if (((controlInformation & 0x0000000f) == 0 || previousMediumState.equals(Idle))) {
                System.out.println("This user can claim a number");
                if (((latestSuccessInformation & 0xfff00000) >>> 20) == 0x000) {
                    number = 0b111;
                    System.out.println("claimed number: " + number);
                    claimedNumbers = 0b111;
                    return new TransmissionInfo(TransmissionType.Data,
                                                (0b111 << 20) | latestSuccessInformation |number);
                }
                if (((latestSuccessInformation & 0xfff00000) >>> 20) == 0b111) {
                    number = 0b101;
                    System.out.println("claimed number: " + number);
                    claimedNumbers = 0b101111;
                    return new TransmissionInfo(TransmissionType.Data,
                                                (0b101 << 23) | latestSuccessInformation | number);
                }
                if (((latestSuccessInformation & 0xfff00000) >>> 20) == 0b101111) {
                    number = 0b011;
                    System.out.println("claimed number: " + number);
                    claimedNumbers = 0b011101111;
                    return new TransmissionInfo(TransmissionType.Data,
                                                (0b011 << 26) | latestSuccessInformation | number);
                }
                if (((latestSuccessInformation & 0xfff00000 )>>> 20) == 0b011101111) {
                    number = 0b110;
                    System.out.println("Claimed number: " + number);
                    claimedNumbers = 0b110011101111;
                    return new TransmissionInfo(TransmissionType.Data,
                                                (0b110 << 29) | latestSuccessInformation | number);
                }
            } else {
                return new TransmissionInfo(TransmissionType.Silent, 0);
            }
        }

        if((controlInformation & 0b111) == number){
            System.out.println("user is next in queue");
            int nextQueue = (controlInformation & 0x000fffff) >>> 3;

                int queueEnd = findEndOfQueue(nextQueue);
                int finalQueue = nextQueue | number << queueEnd;

            return new TransmissionInfo(TransmissionType.Data,
                                        ((controlInformation & 0xfff00000) | finalQueue));
        }

        if(previousMediumState.equals(Collision)) {
            if ((!queueToArray(latestSuccessInformation & 0x000fffff).contains(number)) && (latestSuccessInformation & 0x000fffff) != 0 && localQueueLength >= 1) {

                int latestqueue = latestSuccessInformation & 0x000fffff;
                int queueSize = findEndOfQueue(latestqueue);
                int nextQueue = latestqueue | number << queueSize;
                return new TransmissionInfo(TransmissionType.Data,
                                            ((latestSuccessInformation & 0xfff00000) | nextQueue));
            }

            if(new Random().nextInt(100) < 20 && (!queueToArray(latestSuccessInformation & 0x000fffff).contains(number)) && localQueueLength >= 1){
                return new TransmissionInfo(TransmissionType.Data,
                                            ((latestSuccessInformation & 0xfff00000) | number));
            }
        }

        if(!queueToArray(controlInformation & 0x000fffff).contains(number) && interruptCooldown == 0 && localQueueLength >= 1){
            System.out.println("The user is not in the queue");
            interruptCooldown = 10;
            return new TransmissionInfo(TransmissionType.Data, latestSuccessInformation | number);
        }

        if(previousMediumState.equals(Succes) && (controlInformation & 0x000fffff) == 0){
            return new TransmissionInfo(TransmissionType.Data, latestSuccessInformation );
        }

        return new TransmissionInfo(TransmissionType.Silent, 0);
    }
    public int findEndOfQueue(int queue){
        int result = 0;
        while(queue != 0x0){
            queue = queue >>> 3;
            result ++;
        }
        return result * 3;
    }

    public List<Integer> queueToArray(int queue){
        List<Integer> result = new ArrayList<>();
        while (queue != 0){
            int next = queue & 0b111;
            result.add(next);
            queue = queue >>> 3;
        }
        return result;
    }

}
