.text
    
.global resv
resv:                                        /* Function: resv */
    sub sp, sp, #16
    stmea sp, {r3-r6}
    mov r3, r0
    add r3, r0, #1
    ldr r0, .POOL0_L0_HEAP_START             /* Evaluate Expression */
    lsr r4, r0, #2
resv.L1:                                     /* Evaluate condition */
    ldr r0, [r10, r4, lsl #2]                /* Load from address */
    cmp r0, #0
    beq resv.L2
    ldr r5, [r10, r4, lsl #2]                /* Load from address */
    cmp r5, #0
    bge resv.L4
resv.L6:                                     /* Evaluate condition */
    sub r0, r4, r5
    ldr r6, [r10, r0, lsl #2]                /* Load from address */
    cmp r6, #0
    ble resv.L9
    b resv.L7
resv.L9: 
    cmp r6, #0
    bne resv.L10
    mov r0, #0                               /* Evaluate Expression */
    mov r5, #0
    b resv.L7
resv.L10: 
    add r5, r5, r6
    b resv.L6
resv.L7: 
    str r5, [r10, r4, lsl #2]
    cmp r5, #0
    bne resv.L12
    mov r0, #0                               /* Evaluate Expression */
    push { r0 }
    add r0, r4, r3
    lsl r1, r0, #2
    pop { r0 }
    str r0, [r1]
resv.L12: 
    rsb r5, r5, #0
    cmp r5, r3
    bne resv.L15
    str r3, [r10, r4, lsl #2]
    add r0, r4, #1
    b resv.L19
resv.L15: 
    cmp r5, r3
    ble resv.L16
    sub r5, r5, r3
    str r3, [r10, r4, lsl #2]
    rsb r0, r5, #0
    push { r0 }
    add r0, r4, r3
    lsl r1, r0, #2
    pop { r0 }
    str r0, [r1]
    add r0, r4, #1
    b resv.L19
resv.L16: 
    add r4, r4, r5
    b resv.L3
resv.L4: 
    add r4, r4, r5
resv.L3: 
    b resv.L1
resv.L2: 
    str r3, [r10, r4, lsl #2]
    add r0, r4, #1
resv.L19: 
    ldmfd sp!, {r3-r6}
    bx lr
.POOL0_L0_HEAP_START: .word HEAP_START
