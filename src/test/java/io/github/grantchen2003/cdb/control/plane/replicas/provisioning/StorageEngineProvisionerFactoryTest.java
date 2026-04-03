package io.github.grantchen2003.cdb.control.plane.replicas.provisioning;

import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaType;
import io.github.grantchen2003.cdb.control.plane.replicas.provisioning.redis.RedisStorageEngineProvisioner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class StorageEngineProvisionerFactoryTest {

    @InjectMocks
    private StorageEngineProvisionerFactory factory;

    @Test
    void forType_redis_returnsRedisStorageEngineProvisioner() {
        final Ec2InstanceProvisioner provisioner = factory.forType(ReplicaType.REDIS);
        assertThat(provisioner).isInstanceOf(RedisStorageEngineProvisioner.class);
    }
}