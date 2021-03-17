.version -324703324

.text
.global free
free: 
    sub sp, sp, #12
    stmea sp, {r3, r4, r6}
    mov r3, r0
/* Jump to block head */
    sub r3, r0, #1
/* Load Block Size */
    ldr r4, [r10, r3, lsl #2]
/* Store negated block size */
    rsb r0, r4, #0
    str r0, [r10, r3, lsl #2]
    add r6, r3, r4
free.L1: 
    ldr r0, [r10, r6, lsl #2]
    cmp r0, #0
    bge free.L2
    ldr r4, [r10, r6, lsl #2]
/* Add size to freed block */
    ldr r1, [r10, r3, lsl #2]
    add r0, r1, r4
    str r0, [r10, r3, lsl #2]
/* Shift pointer to next block */
    sub r6, r6, r4
    b free.L1
free.L2: 
    ldmfd sp!, {r3, r4, r6}
    bx lr
