.version 4730757688

.text
    
.global isar
isar:                                        /* Function: isar */
    sub sp, sp, #12
    stmea sp, {r3, r4, lr}
    mov r3, r0
    mov r4, r1
    cmp r0, #0
    bne isar.L0
    mov r0, #0                               /* Evaluate Expression */
    ldmfd sp!, {r3, r4, pc}
isar.L0: 
    cmp r3, r4
    bne isar.L2
    mov r0, #1                               /* Evaluate Expression */
    ldmfd sp!, {r3, r4, pc}
isar.L2: 
    lsr r0, r3, #2
    ldr r0, [r10, r0, lsl #2]                /* Load from address */
    mov r1, r4
    bl isar                                  /* Call isar */
    ldmfd sp!, {r3, r4, pc}
    
.global isa
isa:                                         /* Function: isa */
    push { r3, lr }
    mov r3, r0
    ldr r0, [r10, r3, lsl #2]                /* Load from address */
    bl isar                                  /* Call isar */
    ldmfd sp!, {r3, pc}
