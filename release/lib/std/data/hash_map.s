.include linked_list.s
.include hash.s
    
.data
Map.HashMap: .word 0
    
Map.MapEntry: .word 0
    
.text
    
.global Map.tupleKeyMatcher_P_1_1
Map.tupleKeyMatcher_P_1_1:                   /* Function: Map.tupleKeyMatcher, Provisos: INT, INT */
    sub sp, sp, #12
    stmea sp, {r3-r5}
    mov r3, r0
    mov r4, r1
    mov r5, #0
Map.tupleKeyMatcher.L1: 
    mov r0, #1
    mov r1, #2
    cmp r5, #2
    bge Map.tupleKeyMatcher.L2
    add r0, r3, r5
    ldr r0, [r10, r0, lsl #2]                /* Load from address */
    push { r0 }
    add r0, r4, r5
    ldr r1, [r10, r0, lsl #2]                /* Load from address */
    pop { r0 }
    cmp r0, r1
    beq Map.tupleKeyMatcher.L3
    mov r0, #0                               /* Evaluate Expression */
    b Map.tupleKeyMatcher.L5
Map.tupleKeyMatcher.L3: 
    add r5, r5, #1
    b Map.tupleKeyMatcher.L1
Map.tupleKeyMatcher.L2: 
    mov r0, #1                               /* Evaluate Expression */
Map.tupleKeyMatcher.L5: 
    ldmfd sp!, {r3-r5}
    bx lr
    
.global Map.HashMap.put_P_1_1
Map.HashMap.put_P_1_1:                       /* Function: Map.HashMap.put, Provisos: INT, INT */
    sub sp, sp, #24
    stmea sp, {r3-r6, fp, lr}
    mov fp, sp
    mov r3, r0
    push { r1 }                              /* Push declaration on stack, referenced by addressof. */
    mov r4, r2
    lsl r1, r0, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    push { r0 }
    mov r1, #1
    sub r0, fp, #4
    lsr r0, r0, #2
    bl Hash.hash                             /* Call Hash.hash */
    pop { r1 }
    bl __op_mod                              /* Call __op_mod */
    mov r5, r0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #4]                         /* Load field from struct */
    add r0, r1, r5
    ldr r6, [r10, r0, lsl #2]                /* Load from address */
    ldr r1, .POOL5_L1_NULL                   /* Load null address */
    cmp r6, r1
    bne Map.HashMap.put.L0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #12]                        /* Load field from struct */
    bl List.LinkedList.create_P_1            /* Call List.LinkedList.create */
    mov r6, r0
    lsl r1, r3, #2                           /* Convert to bytes */
    push { r0 }
    ldr r1, [r1, #4]                         /* Load field from struct */
    add r0, r1, r5
    lsl r1, r0, #2
    pop { r0 }
    str r0, [r1]
Map.HashMap.put.L0: 
    push { r4 }
    mov r0, #0
    push { r0 }
    ldr r0, [fp, #-4]
    push { r0 }
    ldr r0, .POOL5_Map.MapEntry
    push { r0 }
    bl init_P_4                              /* Call init */
    mov r3, r0
    mov r0, r6
    push { r3 }
    bl List.LinkedList.add_P_4               /* Call List.LinkedList.add */
    mov sp, fp
    ldmfd sp!, {r3-r6, fp, pc}
.POOL5_Map.MapEntry: .word Map.MapEntry
.POOL5_L1_NULL: .word NULL
    
.global Map.HashMap.replace_P_1_1
Map.HashMap.replace_P_1_1:                   /* Function: Map.HashMap.replace, Provisos: INT, INT */
    sub sp, sp, #24
    stmea sp, {r3-r6, fp, lr}
    mov fp, sp
    mov r3, r0
    push { r1 }                              /* Push declaration on stack, referenced by addressof. */
    mov r4, r2
    lsl r1, r0, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    push { r0 }
    mov r1, #1
    sub r0, fp, #4
    lsr r0, r0, #2
    bl Hash.hash                             /* Call Hash.hash */
    pop { r1 }
    bl __op_mod                              /* Call __op_mod */
    mov r5, r0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #4]                         /* Load field from struct */
    add r0, r1, r5
    ldr r6, [r10, r0, lsl #2]                /* Load from address */
    ldr r1, .POOL6_L1_NULL                   /* Load null address */
    cmp r6, r1
    beq Map.HashMap.replace.L0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #12]
    lsl r1, r1, #2
    ldr r0, [r1, #12]                        /* Load field from struct */
    push { r0 }
    mov r0, #0
    push { r0 }
    ldr r0, [fp, #-4]
    push { r0 }
    ldr r0, .POOL6_Map.MapEntry
    push { r0 }
    lsl r1, r6, #2                           /* Convert to bytes */
    ldr r3, [r1, #4]                         /* Load field from struct */
Map.HashMap.replace.L3:                      /* Evaluate condition */
    ldr r1, .POOL6_L1_NULL                   /* Load null address */
    cmp r3, r1
    beq Map.HashMap.replace.L4
    sub r0, fp, #20                          /* Load parameters */
    lsr r0, r0, #2
    push { r0 }
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    pop { r1 }
    bl Map.tupleKeyMatcher_P_1_1             /* Call Map.tupleKeyMatcher */
    cmp r0, #0
    beq Map.HashMap.replace.L6
    push { r4 }
    mov r0, #0
    push { r0 }
    ldr r0, [fp, #-4]
    push { r0 }
    ldr r0, .POOL6_Map.MapEntry
    push { r0 }
    bl init_P_4                              /* Call init */
    mov r5, r0
    lsl r1, r3, #2                           /* Convert to bytes */
    str r0, [r1, #8]                         /* Store value to struct field */
    b Map.HashMap.replace.L4
Map.HashMap.replace.L6: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r3, [r1, #4]                         /* Load field from struct */
    b Map.HashMap.replace.L3
Map.HashMap.replace.L4: 
    add sp, sp, #16
Map.HashMap.replace.L0: 
    mov sp, fp
    ldmfd sp!, {r3-r6, fp, pc}
.POOL6_Map.MapEntry: .word Map.MapEntry
.POOL6_L1_NULL: .word NULL
    
.global Map.HashMap.get_P_1_1
Map.HashMap.get_P_1_1:                       /* Function: Map.HashMap.get, Provisos: INT, INT */
    sub sp, sp, #20
    stmea sp, {r3-r5, fp, lr}
    mov fp, sp
    mov r3, r0
    push { r1 }                              /* Push declaration on stack, referenced by addressof. */
    lsl r1, r0, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    push { r0 }
    mov r1, #1
    sub r0, fp, #4
    lsr r0, r0, #2
    bl Hash.hash                             /* Call Hash.hash */
    pop { r1 }
    bl __op_mod                              /* Call __op_mod */
    mov r4, r0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #4]                         /* Load field from struct */
    add r0, r1, r4
    ldr r5, [r10, r0, lsl #2]                /* Load from address */
    ldr r1, .POOL7_L1_NULL                   /* Load null address */
    cmp r5, r1
    beq Map.HashMap.get.L0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #12]
    lsl r1, r1, #2
    ldr r0, [r1, #12]                        /* Load field from struct */
    push { r0 }
    mov r0, #0
    push { r0 }
    ldr r0, [fp, #-4]
    push { r0 }
    ldr r0, .POOL7_Map.MapEntry
    push { r0 }
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r4, [r1, #4]                         /* Load field from struct */
Map.HashMap.get.L3:                          /* Evaluate condition */
    ldr r1, .POOL7_L1_NULL                   /* Load null address */
    cmp r4, r1
    beq Map.HashMap.get.L4
    sub r0, fp, #20                          /* Load parameters */
    lsr r0, r0, #2
    push { r0 }
    lsl r1, r4, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    pop { r1 }
    bl Map.tupleKeyMatcher_P_1_1             /* Call Map.tupleKeyMatcher */
    cmp r0, #0
    beq Map.HashMap.get.L6
    lsl r1, r4, #2                           /* Convert to bytes */
    ldr r1, [r1, #8]
    lsl r1, r1, #2
    ldr r0, [r1, #12]                        /* Load field from struct */
    b Map.HashMap.get.L8
Map.HashMap.get.L6: 
    lsl r1, r4, #2                           /* Convert to bytes */
    ldr r4, [r1, #4]                         /* Load field from struct */
    b Map.HashMap.get.L3
Map.HashMap.get.L4: 
    add sp, sp, #16
Map.HashMap.get.L0: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #12]
    lsl r1, r1, #2
    ldr r0, [r1, #12]                        /* Load field from struct */
Map.HashMap.get.L8: 
    mov sp, fp
    ldmfd sp!, {r3-r5, fp, pc}
.POOL7_Map.MapEntry: .word Map.MapEntry
.POOL7_L1_NULL: .word NULL
    
.global Map.HashMap.remove_P_1_1
Map.HashMap.remove_P_1_1:                    /* Function: Map.HashMap.remove, Provisos: INT, INT */
    sub sp, sp, #20
    stmea sp, {r3-r5, fp, lr}
    mov fp, sp
    mov r3, r0
    push { r1 }                              /* Push declaration on stack, referenced by addressof. */
    lsl r1, r0, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    push { r0 }
    mov r1, #1
    sub r0, fp, #4
    lsr r0, r0, #2
    bl Hash.hash                             /* Call Hash.hash */
    pop { r1 }
    bl __op_mod                              /* Call __op_mod */
    mov r4, r0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #4]                         /* Load field from struct */
    add r0, r1, r4
    ldr r5, [r10, r0, lsl #2]                /* Load from address */
    ldr r1, .POOL8_L1_NULL                   /* Load null address */
    cmp r5, r1
    beq Map.HashMap.remove.L0
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #12]
    lsl r1, r1, #2
    ldr r0, [r1, #12]                        /* Load field from struct */
    push { r0 }
    mov r0, #0
    push { r0 }
    ldr r0, [fp, #-4]
    push { r0 }
    ldr r0, .POOL8_Map.MapEntry
    push { r0 }
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r3, [r1, #4]                         /* Load field from struct */
    mov r0, #0                               /* Evaluate Expression */
    mov r4, #0
Map.HashMap.remove.L3:                       /* Evaluate condition */
    ldr r1, .POOL8_L1_NULL                   /* Load null address */
    cmp r3, r1
    beq Map.HashMap.remove.L4
    sub r0, fp, #20                          /* Load parameters */
    lsr r0, r0, #2
    push { r0 }
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    pop { r1 }
    bl Map.tupleKeyMatcher_P_1_1             /* Call Map.tupleKeyMatcher */
    cmp r0, #0
    beq Map.HashMap.remove.L6
    mov r0, r5
    mov r1, r4
    bl List.LinkedList.remove_P_1            /* Call List.LinkedList.remove */
    b Map.HashMap.remove.L4
Map.HashMap.remove.L6: 
    mov r0, r4
    add r4, r4, #1
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r3, [r1, #4]                         /* Load field from struct */
    b Map.HashMap.remove.L3
Map.HashMap.remove.L4: 
    add sp, sp, #16
Map.HashMap.remove.L0: 
    mov sp, fp
    ldmfd sp!, {r3-r5, fp, pc}
.POOL8_Map.MapEntry: .word Map.MapEntry
.POOL8_L1_NULL: .word NULL
    
.global Map.HashMap.create_P_1_1
Map.HashMap.create_P_1_1:                    /* Function: Map.HashMap.create, Provisos: INT, INT */
    sub sp, sp, #20
    stmea sp, {r3-r6, lr}
    mov r3, r0
    mov r4, r1
    bl resv                                  /* Call resv */
    mov r5, r0
    mov r6, #0
Map.HashMap.create.L1: 
    cmp r6, r3
    bge Map.HashMap.create.L2
    ldr r0, .POOL9_L1_NULL                   /* Evaluate Expression */
    push { r0 }
    add r0, r5, r6
    lsl r1, r0, #2
    pop { r0 }
    str r0, [r1]
    add r6, r6, #1
    b Map.HashMap.create.L1
Map.HashMap.create.L2: 
    mov r0, #4                               /* Evaluate Expression */
    bl resv                                  /* Call resv */
    mov r6, r0
    add r0, r6, #3
    str r4, [r10, r0, lsl #2]
    push { r6 }
    push { r3 }
    push { r5 }
    ldr r0, .POOL9_Map.HashMap
    push { r0 }
    bl init_P_4                              /* Call init */
    ldmfd sp!, {r3-r6, pc}
.POOL9_Map.HashMap: .word Map.HashMap
.POOL9_L1_NULL: .word NULL
