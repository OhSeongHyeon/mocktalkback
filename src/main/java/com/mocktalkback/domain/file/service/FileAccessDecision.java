package com.mocktalkback.domain.file.service;

record FileAccessDecision(
    boolean allowed,
    FileDeliveryMode deliveryMode
) {

    static FileAccessDecision deny() {
        return new FileAccessDecision(false, FileDeliveryMode.PROTECTED);
    }

    static FileAccessDecision publicAccess() {
        return new FileAccessDecision(true, FileDeliveryMode.PUBLIC);
    }

    static FileAccessDecision protectedAccess() {
        return new FileAccessDecision(true, FileDeliveryMode.PROTECTED);
    }
}
