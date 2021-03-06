package com.facebook.presto.elasticsearch;

import com.facebook.presto.elasticsearch.conf.ElasticsearchSessionProperties;
import com.facebook.presto.elasticsearch.io.ElasticsearchPageSinkProvider;
import com.facebook.presto.elasticsearch.io.ElasticsearchPageSourceProvider;
import com.facebook.presto.elasticsearch.model.ElasticsearchTransactionHandle;
import com.facebook.presto.spi.connector.Connector;
import com.facebook.presto.spi.connector.ConnectorMetadata;
import com.facebook.presto.spi.connector.ConnectorPageSinkProvider;
import com.facebook.presto.spi.connector.ConnectorPageSourceProvider;
import com.facebook.presto.spi.connector.ConnectorSplitManager;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
import com.facebook.presto.spi.session.PropertyMetadata;
import com.facebook.presto.spi.transaction.IsolationLevel;
import io.airlift.bootstrap.LifeCycleManager;
import io.airlift.log.Logger;

import javax.inject.Inject;

import java.util.List;

import static com.facebook.presto.spi.transaction.IsolationLevel.READ_UNCOMMITTED;
import static com.facebook.presto.spi.transaction.IsolationLevel.checkConnectorSupports;
import static java.util.Objects.requireNonNull;

public class ElasticsearchConnector
        implements Connector
{
    private static final Logger LOG = Logger.get(ElasticsearchConnector.class);

    private final LifeCycleManager lifeCycleManager;
    private final ElasticsearchMetadata metadata;
    private final ElasticsearchSplitManager splitManager;
    private final ElasticsearchPageSourceProvider pageSourceProvider;
    private final ElasticsearchPageSinkProvider pageSinkProvider;
    private final ElasticsearchSessionProperties sessionProperties;
//    private final ElasticsearchTableProperties tableProperties;

    @Inject
    public ElasticsearchConnector(
            LifeCycleManager lifeCycleManager,
            ElasticsearchMetadata metadata,
            ElasticsearchSplitManager splitManager,
            ElasticsearchPageSourceProvider pageSourceProvider,
            ElasticsearchPageSinkProvider pageSinkProvider,
            ElasticsearchSessionProperties sessionProperties)
    {
        this.lifeCycleManager = requireNonNull(lifeCycleManager, "lifeCycleManager is null");
        this.metadata = requireNonNull(metadata, "metadata is null");
        this.splitManager = requireNonNull(splitManager, "splitManager is null");
        this.pageSourceProvider = requireNonNull(pageSourceProvider, "pageSourceProvider is null");
        this.pageSinkProvider = requireNonNull(pageSinkProvider, "pageSinkProvider is null");
        this.sessionProperties = requireNonNull(sessionProperties, "sessionProperties is null");
//        this.tableProperties = requireNonNull(tableProperties, "tableProperties is null");
    }

    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly)
    {
        checkConnectorSupports(READ_UNCOMMITTED, isolationLevel);
        return new ElasticsearchTransactionHandle();
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorTransactionHandle transactionHandle)
    {
        return this.metadata;
    }

    @Override
    public ConnectorSplitManager getSplitManager()
    {
        return this.splitManager;
    }

    @Override
    public ConnectorPageSourceProvider getPageSourceProvider()
    {
        return this.pageSourceProvider;
    }

    @Override
    public ConnectorPageSinkProvider getPageSinkProvider()
    {
        return this.pageSinkProvider;
    }

    @Override
    public List<PropertyMetadata<?>> getSessionProperties()
    {
        return sessionProperties.getSessionProperties();
    }
}
