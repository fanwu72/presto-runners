package com.facebook.presto.kudu;

import com.facebook.presto.spi.ConnectorSession;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.ConnectorSplitSource;
import com.facebook.presto.spi.ConnectorTableLayoutHandle;
import com.facebook.presto.spi.FixedSplitSource;
import com.facebook.presto.spi.HostAddress;
import com.facebook.presto.spi.NodeManager;
import com.facebook.presto.spi.connector.ConnectorSplitManager;
import com.facebook.presto.spi.connector.ConnectorTransactionHandle;
import com.facebook.presto.spi.predicate.TupleDomain;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduScanToken;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class KuduSplitManager
        implements ConnectorSplitManager
{
    private final NodeManager nodeManager;
    private final KuduClientManager kuduClientManager;

    @Inject
    public KuduSplitManager(NodeManager nodeManager, KuduClientManager kuduClientManager)
    {
        this.nodeManager = requireNonNull(nodeManager, "nodeManager is null");
        this.kuduClientManager = requireNonNull(kuduClientManager, "kuduClientManager is null");
    }

    @Override
    public ConnectorSplitSource getSplits(ConnectorTransactionHandle transactionHandle, ConnectorSession session, ConnectorTableLayoutHandle layout, SplitSchedulingStrategy splitSchedulingStrategy)
    {
        KuduTableLayoutHandle layoutHandle = Types.checkType(layout, KuduTableLayoutHandle.class, "layout");
        KuduTableHandle tableHandle = layoutHandle.getTable();
        KuduClient kuduClient = kuduClientManager.getClient();

        List<KuduScanToken> tokens = kuduClientManager.newScanTokenBuilder(kuduClient, tableHandle.getSchemaTableName().getTableName()).build();

        TupleDomain<KuduColumnHandle> effectivePredicate = layoutHandle.getConstraint()
                .transform(handle -> Types.checkType(handle, KuduColumnHandle.class, "columnHandle"));

        ImmutableList.Builder<ConnectorSplit> builder = ImmutableList.builder();

        for (int i = 0; i < tokens.size(); i++) {
//            nodeManager.getWorkerNodes()
            List<HostAddress> hostAddresses = nodeManager.getWorkerNodes().stream()
                    .map(node -> node.getHostAndPort()).collect(Collectors.toList());
            ConnectorSplit split = new KuduSplit(hostAddresses, tableHandle.getSchemaTableName(), i, effectivePredicate);
            builder.add(split);
        }

        kuduClientManager.close(kuduClient);
        return new FixedSplitSource(builder.build());
    }
}
