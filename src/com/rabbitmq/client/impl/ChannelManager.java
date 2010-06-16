//   The contents of this file are subject to the Mozilla Public License
//   Version 1.1 (the "License"); you may not use this file except in
//   compliance with the License. You may obtain a copy of the License at
//   http://www.mozilla.org/MPL/
//
//   Software distributed under the License is distributed on an "AS IS"
//   basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//   License for the specific language governing rights and limitations
//   under the License.
//
//   The Original Code is RabbitMQ.
//
//   The Initial Developers of the Original Code are LShift Ltd,
//   Cohesive Financial Technologies LLC, and Rabbit Technologies Ltd.
//
//   Portions created before 22-Nov-2008 00:00:00 GMT by LShift Ltd,
//   Cohesive Financial Technologies LLC, or Rabbit Technologies Ltd
//   are Copyright (C) 2007-2008 LShift Ltd, Cohesive Financial
//   Technologies LLC, and Rabbit Technologies Ltd.
//
//   Portions created by LShift Ltd are Copyright (C) 2007-2010 LShift
//   Ltd. Portions created by Cohesive Financial Technologies LLC are
//   Copyright (C) 2007-2010 Cohesive Financial Technologies
//   LLC. Portions created by Rabbit Technologies Ltd are Copyright
//   (C) 2007-2010 Rabbit Technologies Ltd.
//
//   All Rights Reserved.
//
//   Contributor(s): ______________________________________.
//
package com.rabbitmq.client.impl;

import com.rabbitmq.client.ShutdownSignalException;

<<<<<<< local
import java.util.*;
=======
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.utility.IntAllocator;
>>>>>>> other

/**
 * Manages a set of channels, indexed by channel number.
 */

public class ChannelManager {
    /** Mapping from channel number to AMQChannel instance */
    private final Map<Integer, ChannelN> _channelMap =
        Collections.synchronizedMap(new HashMap<Integer, ChannelN>());
    private final IntAllocator channelNumberAllocator;

    /** Maximum channel number available on this connection. */
    public final int _channelMax;

    public int getChannelMax(){
      return _channelMax;
    }

    public ChannelManager(int channelMax) {
        if (channelMax == 0) {
            // The framing encoding only allows for unsigned 16-bit integers
            // for the channel number
            channelMax = (1 << 16) - 1;
        }
        _channelMax = channelMax;
        channelNumberAllocator = new IntAllocator(1, channelMax);
    }


    /**
     * Public API - Looks up an existing channel associated with this connection.
     * @param channelNumber the number of the required channel
     * @return the relevant channel descriptor
     * @throws UnknownChannelException if there is no Channel associated with the
     *         required channel number.
     */
    public ChannelN getChannel(int channelNumber) {
        ChannelN result = _channelMap.get(channelNumber);
        if(result == null) throw new UnknownChannelException(channelNumber);
        return result;
    }

    public void handleSignal(ShutdownSignalException signal) {
        Set<ChannelN> channels;
        synchronized(_channelMap) {
            channels = new HashSet<ChannelN>(_channelMap.values());
        }
        for (ChannelN channel : channels) {
            disconnectChannel(channel);
            channel.processShutdownSignal(signal, true, true);
        }
    }

<<<<<<< local
    public synchronized ChannelN createChannel(AMQConnection connection) {
        int channelNumber = allocateChannelNumber(getChannelMax());
=======
    public synchronized ChannelN createChannel(AMQConnection connection) throws IOException {
        int channelNumber = channelNumberAllocator.allocate();
>>>>>>> other
        if (channelNumber == -1) {
            return null;
        }
        return createChannelInternal(connection, channelNumber);
    }

<<<<<<< local
    public synchronized ChannelN createChannel(AMQConnection connection, int channelNumber) {
=======
    public synchronized ChannelN createChannel(AMQConnection connection, int channelNumber) throws IOException {
        if(channelNumberAllocator.reserve(channelNumber))
            return createChannelInternal(connection, channelNumber);
        else
            return null;
    }

    private synchronized ChannelN createChannelInternal(AMQConnection connection, int channelNumber) throws IOException {
        if (_channelMap.containsKey(channelNumber)) {
            // That number's already allocated! Can't do it
            // This should never happen unless something has gone
            // badly wrong with our implementation.
            throw new IllegalStateException("We have attempted to "
              + "create a channel with a number that is already in "
              + "use. This should never happen. Please report this as a bug.");
        }
>>>>>>> other
        ChannelN ch = new ChannelN(connection, channelNumber);
        addChannel(ch);
        ch.open(); // now that it's been added to our internal tables
        return ch;
    }

    private void addChannel(ChannelN chan) {
        _channelMap.put(chan.getChannelNumber(), chan);
    }

    /**
     * Remove the argument channel from the channel map. 
     * This method must be safe to call multiple times on the same channel. If 
     * it is not then things go badly wrong.
     */
    public synchronized void disconnectChannel(ChannelN channel) {
        int channelNumber = channel.getChannelNumber();
       
        // Warning, here be dragons. Not great big ones, but little baby ones
        // which will nibble on your toes and occasionally trip you up when 
        // you least expect it. 
        // Basically, there's a race that can end us up here. It almost never 
        // happens, but it's easier to repair it when it does than prevent it 
        // from happening in the first place. 
        // If we end up doing a Channel.close in one thread and a Channel.open
        // with the same channel number in another, the two can overlap in such
        // a way as to cause disconnectChannel on the old channel to try to 
        // remove the new one. Ideally we would fix this race at the source,
        // but it's much easier to just catch it here.   
        synchronized (_channelMap) {
          ChannelN existing = _channelMap.remove(channelNumber);
          // Nothing to do here. Move along. 
          if (existing == null) return;
          // Oops, we've gone and stomped on someone else's channel. Put it back
          // and pretend we didn't touch it. 
          else if (existing != channel) {
            _channelMap.put(channelNumber, existing);
            return;
          }
          channelNumberAllocator.free(channelNumber);
        } 
    }
}
