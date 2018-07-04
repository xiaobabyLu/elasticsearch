/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.core.ml.action;

import org.elasticsearch.action.Action;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.client.ElasticsearchClient;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ObjectParser;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.core.ml.job.config.MlFilter;
import org.elasticsearch.xpack.core.ml.job.messages.Messages;
import org.elasticsearch.xpack.core.ml.utils.ExceptionsHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;


public class UpdateFilterAction extends Action<UpdateFilterAction.Request, PutFilterAction.Response, UpdateFilterAction.RequestBuilder> {

    public static final UpdateFilterAction INSTANCE = new UpdateFilterAction();
    public static final String NAME = "cluster:admin/xpack/ml/filters/update";

    private UpdateFilterAction() {
        super(NAME);
    }

    @Override
    public RequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new RequestBuilder(client);
    }

    @Override
    public PutFilterAction.Response newResponse() {
        return new PutFilterAction.Response();
    }

    public static class Request extends ActionRequest implements ToXContentObject {

        public static final ParseField ADD_ITEMS = new ParseField("add_items");
        public static final ParseField REMOVE_ITEMS = new ParseField("remove_items");

        private static final ObjectParser<Request, Void> PARSER = new ObjectParser<>(NAME, Request::new);

        static {
            PARSER.declareString((request, filterId) -> request.filterId = filterId, MlFilter.ID);
            PARSER.declareStringOrNull(Request::setDescription, MlFilter.DESCRIPTION);
            PARSER.declareStringArray(Request::setAddItems, ADD_ITEMS);
            PARSER.declareStringArray(Request::setRemoveItems, REMOVE_ITEMS);
        }

        public static Request parseRequest(String filterId, XContentParser parser) {
            Request request = PARSER.apply(parser, null);
            if (request.filterId == null) {
                request.filterId = filterId;
            } else if (!Strings.isNullOrEmpty(filterId) && !filterId.equals(request.filterId)) {
                // If we have both URI and body filter ID, they must be identical
                throw new IllegalArgumentException(Messages.getMessage(Messages.INCONSISTENT_ID, MlFilter.ID.getPreferredName(),
                        request.filterId, filterId));
            }
            return request;
        }

        private String filterId;
        @Nullable
        private String description;
        private SortedSet<String> addItems = Collections.emptySortedSet();
        private SortedSet<String> removeItems = Collections.emptySortedSet();

        public Request() {
        }

        public Request(String filterId) {
            this.filterId = ExceptionsHelper.requireNonNull(filterId, MlFilter.ID.getPreferredName());
        }

        public String getFilterId() {
            return filterId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public SortedSet<String> getAddItems() {
            return addItems;
        }

        public void setAddItems(Collection<String> addItems) {
            this.addItems = new TreeSet<>(ExceptionsHelper.requireNonNull(addItems, ADD_ITEMS.getPreferredName()));
        }

        public SortedSet<String> getRemoveItems() {
            return removeItems;
        }

        public void setRemoveItems(Collection<String> removeItems) {
            this.removeItems = new TreeSet<>(ExceptionsHelper.requireNonNull(removeItems, REMOVE_ITEMS.getPreferredName()));
        }

        public boolean isNoop() {
            return description == null && addItems.isEmpty() && removeItems.isEmpty();
        }

        @Override
        public ActionRequestValidationException validate() {
            return null;
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
            filterId = in.readString();
            description = in.readOptionalString();
            addItems = new TreeSet<>(Arrays.asList(in.readStringArray()));
            removeItems = new TreeSet<>(Arrays.asList(in.readStringArray()));
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
            out.writeString(filterId);
            out.writeOptionalString(description);
            out.writeStringArray(addItems.toArray(new String[addItems.size()]));
            out.writeStringArray(removeItems.toArray(new String[removeItems.size()]));
        }

        @Override
        public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
            builder.startObject();
            builder.field(MlFilter.ID.getPreferredName(), filterId);
            if (description != null) {
                builder.field(MlFilter.DESCRIPTION.getPreferredName(), description);
            }
            if (addItems.isEmpty() == false) {
                builder.field(ADD_ITEMS.getPreferredName(), addItems);
            }
            if (removeItems.isEmpty() == false) {
                builder.field(REMOVE_ITEMS.getPreferredName(), removeItems);
            }
            builder.endObject();
            return builder;
        }

        @Override
        public int hashCode() {
            return Objects.hash(filterId, description, addItems, removeItems);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Request other = (Request) obj;
            return Objects.equals(filterId, other.filterId)
                    && Objects.equals(description, other.description)
                    && Objects.equals(addItems, other.addItems)
                    && Objects.equals(removeItems, other.removeItems);
        }
    }

    public static class RequestBuilder extends ActionRequestBuilder<Request, PutFilterAction.Response, RequestBuilder> {

        public RequestBuilder(ElasticsearchClient client) {
            super(client, INSTANCE, new Request());
        }
    }
}