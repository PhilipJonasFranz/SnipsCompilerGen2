.text
    
.global free
free:                                        /* Function: free */
    sub sp, sp, #12
    stmea sp, {r3, r4, r6}
    mov r3, r0
    sub r3, r0, #1
    ldr r4, [r10, r3, lsl #2]                /* Load from address */
    rsb r0, r4, #0
    str r0, [r10, r3, lsl #2]
    add r6, r3, r4
free.L1:                                     /* Evaluate condition */
    ldr r0, [r10, r6, lsl #2]                /* Load from address */
    cmp r0, #0
    bge free.L2
    ldr r4, [r10, r6, lsl #2]                /* Load from address */
    ldr r1, [r10, r3, lsl #2]                /* Load from address */
    add r0, r1, r4
    str r0, [r10, r3, lsl #2]
    sub r6, r6, r4
    b free.L1
free.L2: 
    ldmfd sp!, {r3, r4, r6}
    bx lr
