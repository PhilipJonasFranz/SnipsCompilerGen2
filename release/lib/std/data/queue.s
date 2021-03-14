.data
Queue.CyclicQueue: .word 0
    
.text
    
.global Queue.CyclicQueue.destroy_P_2
Queue.CyclicQueue.destroy_P_2:               /* Function: Queue.CyclicQueue.destroy, Provisos: INT[2] */
    push { r3, lr }
    mov r3, r0
    lsl r1, r0, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    bl free                                  /* Call free */
    mov r0, r3                               /* Load parameters */
    bl free                                  /* Call free */
    ldmfd sp!, {r3, pc}
    
.global Queue.CyclicQueue.enqueue_P_2
Queue.CyclicQueue.enqueue_P_2:               /* Function: Queue.CyclicQueue.enqueue, Provisos: INT[2] */
    sub sp, sp, #16
    stmea sp, {r3, r4, fp, lr}
    mov fp, sp
    mov r3, r0
    lsl r1, r0, #2                           /* Convert to bytes */
    ldr r0, [r1, #20]                        /* Load field from struct */
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #24]                        /* Load field from struct */
    cmp r0, r1
    moveq r0, #1
    movne r0, #0
    lsl r1, r3, #2                           /* Convert to bytes */
    push { r0 }
    ldr r0, [r1, #28]                        /* Load field from struct */
    cmp r0, #0
    moveq r0, #1
    movne r0, #0
    mov r1, r0
    pop { r0 }
    adds r1, r1, #0
    movne r1, #1
    cmp r0, #0
    movne r0, r1
    moveq r0, #0
    cmp r0, #0
    beq Queue.CyclicQueue.enqueue.L1
    b Queue.CyclicQueue.enqueue.L3
Queue.CyclicQueue.enqueue.L1: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #20]                        /* Load field from struct */
    lsl r4, r0, #1
    ldr r0, [fp, #20]                        /* Evaluate Expression */
    ldr r1, [fp, #16]
    stmfd sp!, {r0, r1}
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #4]                         /* Load field from struct */
    add r0, r1, r4
    lsl r1, r0, #2
    pop { r0 }
    str r0, [r1]
    pop { r0 }
    str r0, [r1, #4]
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #16]                        /* Load field from struct */
    push { r0 }
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #20]                        /* Load field from struct */
    add r0, r1, #1
    pop { r1 }
    bl __op_mod                              /* Call __op_mod */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r0, [r1, #20]                        /* Store value to struct field */
    mov r0, #0                               /* Evaluate Expression */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r0, [r1, #28]                        /* Store value to struct field */
Queue.CyclicQueue.enqueue.L3: 
    mov sp, fp
    ldmfd sp!, {r3, r4, fp, lr}
    add sp, sp, #8
    bx lr
    
.global Queue.CyclicQueue.dequeue_P_2
Queue.CyclicQueue.dequeue_P_2:               /* Function: Queue.CyclicQueue.dequeue, Provisos: INT[2] */
    sub sp, sp, #12
    stmea sp, {r3, fp, lr}
    mov fp, sp
    mov r3, r0
    lsl r1, r0, #2                           /* Convert to bytes */
    ldr r0, [r1, #28]                        /* Load field from struct */
    cmp r0, #0
    beq Queue.CyclicQueue.dequeue.L1
    lsl r1, r3, #2                           /* Convert to bytes */
    add r1, r1, #8
    ldr r0, [r1, #4]
    ldr r2, [r1]
    stmfd sp!, {r0, r2}
    b Queue.CyclicQueue.dequeue.L5
Queue.CyclicQueue.dequeue.L1: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    lsl r1, r3, #2                           /* Convert to bytes */
    push { r0 }
    ldr r0, [r1, #24]                        /* Load field from struct */
    pop { r1 }
    lsl r2, r0, #1
    add r0, r1, r2
    lsl r1, r0, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]
    push { r0 }
    ldr r0, [r1]
    lsl r1, r3, #2                           /* Convert to bytes */
    push { r0 }
    ldr r0, [r1, #16]                        /* Load field from struct */
    push { r0 }
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #24]                        /* Load field from struct */
    add r0, r1, #1
    pop { r1 }
    bl __op_mod                              /* Call __op_mod */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r0, [r1, #24]                        /* Store value to struct field */
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #24]                        /* Load field from struct */
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #20]                        /* Load field from struct */
    cmp r0, r1
    bne Queue.CyclicQueue.dequeue.L3
    mov r0, #1                               /* Evaluate Expression */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r0, [r1, #28]                        /* Store value to struct field */
Queue.CyclicQueue.dequeue.L3: 
    ldr r0, [fp, #-4]                        /* Evaluate Expression */
    ldr r1, [fp, #-8]
    stmfd sp!, {r0, r1}
Queue.CyclicQueue.dequeue.L5: 
    mov r2, sp
    mov sp, fp
    ldmfd sp!, {r3, fp, lr}
    mov r0, #8
    add r1, r2, #8
    add r10, pc, #8                          /* Setup return address for routine */
    b _routine_stack_copy_
    mov r10, #0
    bx lr
    
.global Queue.CyclicQueue.create_P_2
Queue.CyclicQueue.create_P_2:                /* Function: Queue.CyclicQueue.create, Provisos: INT[2] */
    sub sp, sp, #16
    stmea sp, {r3, r4, fp, lr}
    mov fp, sp
    mov r3, r0
    mov r0, #2                               /* Evaluate Expression */
    mul r0, r0, r3
    bl resv                                  /* Call resv */
    mov r4, r0
    mov r0, #1                               /* Evaluate Expression */
    mov r1, #0
    mov r2, #0
    stmfd sp!, {r0-r2}
    push { r3 }
    ldr r0, [fp, #20]
    ldr r1, [fp, #16]
    stmfd sp!, {r0, r1}
    push { r4 }
    ldr r0, .POOL1_Queue.CyclicQueue
    push { r0 }
    bl init_P_8                              /* Call init */
    mov sp, fp
    ldmfd sp!, {r3, r4, fp, lr}
    add sp, sp, #8
    bx lr
.POOL1_Queue.CyclicQueue: .word Queue.CyclicQueue
