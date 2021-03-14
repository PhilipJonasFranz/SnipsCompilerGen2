.text
    
.global Hash.hash
Hash.hash:                                   /* Function: Hash.hash */
    sub sp, sp, #20
    stmea sp, {r3-r7}
    mov r3, r0
    mov r4, r1
    ldr r5, .POOL9_LIT_5381                  /* Evaluate Expression */
    mov r6, #0
Hash.hash.L1: 
    cmp r6, r4
    bge Hash.hash.L2
    add r0, r3, r6
    ldr r7, [r10, r0, lsl #2]                /* Load from address */
    lsl r2, r5, #6
    add r0, r7, r2
    lsl r2, r5, #16
    add r1, r0, r2
    sub r5, r1, r5
    add r6, r6, #1
    b Hash.hash.L1
Hash.hash.L2: 
    mov r0, r5                               /* Evaluate Expression */
    ldmfd sp!, {r3-r7}
    bx lr
.POOL9_LIT_5381: .word 5381
