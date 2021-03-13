.text
    
.global isa
isa:                                         /* Function: isa */
    sub sp, sp, #12
    stmea sp, {r3, r4, lr}
    mov r3, r0
    mov r4, r1
    cmp r0, #0
    bne isa.L0
    mov r0, #0                               /* Evaluate Expression */
    ldmfd sp!, {r3, r4, pc}
isa.L0: 
    cmp r3, r4
    bne isa.L2
    mov r0, #1                               /* Evaluate Expression */
    ldmfd sp!, {r3, r4, pc}
isa.L2: 
    lsr r0, r3, #2
    ldr r0, [r10, r0, lsl #2]                /* Load from address */
    mov r1, r4
    bl isa                                   /* Call isa */
    ldmfd sp!, {r3, r4, pc}
