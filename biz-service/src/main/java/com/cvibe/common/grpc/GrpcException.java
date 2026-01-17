package com.cvibe.common.grpc;

/**
 * gRPC 服务调用异常
 */
public class GrpcException extends RuntimeException {

    public GrpcException(String message) {
        super(message);
    }

    public GrpcException(String message, Throwable cause) {
        super(message, cause);
    }
}
