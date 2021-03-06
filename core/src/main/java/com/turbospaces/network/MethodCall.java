/**
 * Copyright (C) 2011-2012 Andrey Borisov <aandrey.borisov@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.turbospaces.network;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.turbospaces.api.SpaceOperation;
import com.turbospaces.spaces.SpaceMethodsMapping;

/**
 * POJO for network communications
 * 
 * @since 0.1
 */
public class MethodCall {
    protected short methodId;
    private byte[] responseBody;
    private long correlationId;
    private String exceptionAsString;

    /**
     * @return the unique method id which can be used both by client and server to identify method
     */
    public short getMethodId() {
        return methodId;
    }

    /**
     * associate response in form of bytes array with request object
     * 
     * @param responseBody
     *            response in serialized form
     */
    public void setResponseBody(final byte[] responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * @return response from server in form of bytes-array
     */
    public byte[] getResponseBody() {
        return responseBody;
    }

    /**
     * set server exception (if any) that caused server processing failure
     * 
     * @param exception
     *            execution exception of server side
     */
    public void setException(final Throwable exception) {
        this.exceptionAsString = Throwables.getStackTraceAsString( exception );
    }

    /**
     * @return exception as string if any
     */
    public String getExceptionAsString() {
        return exceptionAsString;
    }

    /**
     * @return the correlation id for this method call
     */
    long getCorrelationId() {
        return correlationId;
    }

    /**
     * assign request-response correlation id
     * 
     * @param correlationId
     */
    void setCorrelationId(final long correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String toString() {
        SpaceMethodsMapping spaceMethodsMapping = SpaceMethodsMapping.values()[getMethodId()];
        return Objects.toStringHelper( this ).add( "method", spaceMethodsMapping.name() ).toString();
    }

    @SuppressWarnings("javadoc")
    public static final class GetSizeMethodCall extends MethodCall {
        public GetSizeMethodCall() {
            super();
            methodId = (short) SpaceMethodsMapping.SIZE.ordinal();
        }
    }

    @SuppressWarnings("javadoc")
    public static final class GetMbUsedMethodCall extends MethodCall {
        public GetMbUsedMethodCall() {
            super();
            methodId = (short) SpaceMethodsMapping.MB_USED.ordinal();
        }
    }

    @SuppressWarnings("javadoc")
    public static final class EvictAllMethodCall extends MethodCall {
        public EvictAllMethodCall() {
            super();
            methodId = (short) SpaceMethodsMapping.EVICT_ALL.ordinal();
        }
    }

    @SuppressWarnings("javadoc")
    public static final class EvictPercentageMethodCall extends MethodCall {
        private int percentage;

        public EvictPercentageMethodCall() {
            super();
            methodId = (short) SpaceMethodsMapping.EVICT_PERCENTAGE.ordinal();
        }

        public int getPercentage() {
            return percentage;
        }

        public void setPercentage(final int percentage) {
            this.percentage = percentage;
        }
    }

    @SuppressWarnings("javadoc")
    public static final class EvictElementsMethodCall extends MethodCall {
        private long elements;

        public EvictElementsMethodCall() {
            super();
            methodId = (short) SpaceMethodsMapping.EVICT_ELEMENTS.ordinal();
        }

        public long getElements() {
            return elements;
        }

        public void setElements(final long elements) {
            this.elements = elements;
        }
    }

    @SuppressWarnings("javadoc")
    public static final class GetSpaceTopologyMethodCall extends MethodCall {
        public GetSpaceTopologyMethodCall() {
            super();
            methodId = (short) SpaceMethodsMapping.SPACE_TOPOLOGY.ordinal();
        }
    }

    @SuppressWarnings("javadoc")
    public static final class BeginTransactionMethodCall extends MethodCall {
        private long transactionTimeout;

        public BeginTransactionMethodCall() {
            super();
            methodId = (short) SpaceMethodsMapping.BEGIN_TRANSACTION.ordinal();
        }

        public long getTransactionTimeout() {
            return transactionTimeout;
        }

        public void setTransactionTimeout(final long transactionTimeout) {
            Preconditions.checkArgument( transactionTimeout > 0, "transaction timeout must be positive" );
            this.transactionTimeout = transactionTimeout;
        }
    }

    @SuppressWarnings("javadoc")
    public static class CommitRollbackMethodCall extends MethodCall {
        private long transactionId;

        CommitRollbackMethodCall() {}

        public CommitRollbackMethodCall(final boolean commit) {
            methodId = (short) ( commit ? SpaceMethodsMapping.COMMIT_TRANSACTION : SpaceMethodsMapping.ROLLBACK_TRANSACTION ).ordinal();
        }

        public long getTransactionId() {
            return transactionId;
        }

        public void setTransactionId(final long transactionId) {
            this.transactionId = transactionId;
        }
    }

    @SuppressWarnings("javadoc")
    public static abstract class ModifyMethodCall extends CommitRollbackMethodCall {
        private byte[] entity;
        private int modifiers;
        private int timeout;

        public int getModifiers() {
            return modifiers;
        }

        public void setModifiers(final int modifiers) {
            this.modifiers = modifiers;
        }

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(final int timeout) {
            this.timeout = timeout;
        }

        public byte[] getEntity() {
            return entity;
        }

        public void setEntity(final byte[] entity) {
            this.entity = entity;
        }

        public void reset() {
            entity = null;
            modifiers = 0;
            timeout = 0;
        }
    }

    @SuppressWarnings("javadoc")
    public static final class NotifyListenerMethodCall extends ModifyMethodCall {
        private SpaceOperation operation;

        public NotifyListenerMethodCall() {
            methodId = (short) SpaceMethodsMapping.NOTIFY.ordinal();
        }

        public SpaceOperation getOperation() {
            return operation;
        }

        public void setOperation(final SpaceOperation operation) {
            this.operation = operation;
        }
    }

    /**
     * remote jspace write method class.
     * 
     * @since 0.1
     */
    @SuppressWarnings("javadoc")
    public static final class WriteMethodCall extends ModifyMethodCall {
        private int timeToLive;

        public WriteMethodCall() {
            super();
            methodId = (short) SpaceMethodsMapping.WRITE.ordinal();
        }

        public int getTimeToLive() {
            return timeToLive;
        }

        public void setTimeToLive(final int timeToLive) {
            this.timeToLive = timeToLive;
        }

        @Override
        public void reset() {
            super.reset();
            timeToLive = 0;
        }
    }

    /**
     * remote jspace read fetch method class.
     * 
     * @since 0.1
     * 
     */
    @SuppressWarnings("javadoc")
    public static final class FetchMethodCall extends ModifyMethodCall {
        private int maxResults;

        public FetchMethodCall() {
            super();
            methodId = (short) SpaceMethodsMapping.FETCH.ordinal();
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(final int maxResults) {
            this.maxResults = maxResults;
        }

        @Override
        public void reset() {
            super.reset();
            maxResults = 0;
        }
    }
}
