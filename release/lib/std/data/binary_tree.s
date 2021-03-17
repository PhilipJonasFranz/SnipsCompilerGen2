.version 28446757266

.data
Tree.TreeNode: .word 0

.text
.global Tree.TreeNode.insert_P_1
Tree.TreeNode.insert_P_1:                    /* Function: Tree.TreeNode.insert, Provisos: INT */
    sub sp, sp, #28
    stmea sp, {r3-r8, lr}
    mov r3, r0
    mov r4, r1
    mov r5, r2
    push { r5 }
    ldr r0, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    stmfd sp!, {r0, r1}
    ldr r0, .POOL@-1914853343_0_Tree.TreeNode
    push { r0 }
    bl init_P_4                              /* Call init */
    mov r6, r0
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    cmp r3, r1
    bne Tree.TreeNode.insert.L1
    mov r0, r6                               /* Evaluate Expression */
    ldmfd sp!, {r3-r8, pc}
Tree.TreeNode.insert.L1: 
    mov r0, r3                               /* Evaluate Expression */
    mov r7, r3
Tree.TreeNode.insert.L4:                     /* Evaluate condition */
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    cmp r7, r1
    ldmfdeq sp!, {r3-r8, pc}
    lsl r1, r7, #2                           /* Convert to bytes */
    ldr r0, [r1, #12]                        /* Load field from struct */
    mov r1, r5
    add lr, pc, #8
    mov pc, r4
    mov r8, r0
    cmp r8, #0
    bne Tree.TreeNode.insert.L7
    mov r0, r3                               /* Evaluate Expression */
    ldmfd sp!, {r3-r8, pc}
Tree.TreeNode.insert.L7: 
    cmp r8, #1
    bne Tree.TreeNode.insert.L8
    lsl r1, r7, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    cmp r0, r1
    bne Tree.TreeNode.insert.L10
    lsl r1, r7, #2                           /* Convert to bytes */
    str r6, [r1, #4]                         /* Store value to struct field */
    mov r0, r3                               /* Evaluate Expression */
    ldmfd sp!, {r3-r8, pc}
Tree.TreeNode.insert.L10: 
    lsl r1, r7, #2                           /* Convert to bytes */
    ldr r7, [r1, #4]                         /* Load field from struct */
    b Tree.TreeNode.insert.L6
Tree.TreeNode.insert.L8: 
    lsl r1, r7, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    ldr r1, .POOL@-1914853343_0_L1_NULL      /* Load null address */
    cmp r0, r1
    bne Tree.TreeNode.insert.L14
    lsl r1, r7, #2                           /* Convert to bytes */
    str r6, [r1, #8]                         /* Store value to struct field */
    mov r0, r3                               /* Evaluate Expression */
    ldmfd sp!, {r3-r8, pc}
Tree.TreeNode.insert.L14: 
    lsl r1, r7, #2                           /* Convert to bytes */
    ldr r7, [r1, #8]                         /* Load field from struct */
Tree.TreeNode.insert.L6: 
    b Tree.TreeNode.insert.L4
    ldmfd sp!, {r3-r8, pc}
.POOL@-1914853343_0_Tree.TreeNode: .word Tree.TreeNode
.POOL@-1914853343_0_L1_NULL: .word NULL
    
.global Tree.TreeNode.delete_P_1
Tree.TreeNode.delete_P_1:                    /* Function: Tree.TreeNode.delete, Provisos: INT */
    sub sp, sp, #20
    stmea sp, {r3-r6, lr}
    mov r3, r0
    mov r4, r1
    mov r5, r2
    ldr r1, .POOL@-1914853343_1_L1_NULL      /* Load null address */
    cmp r0, r1
    bne Tree.TreeNode.delete.L0
    ldr r0, .POOL@-1914853343_1_L1_NULL      /* Evaluate Expression */
    ldmfd sp!, {r3-r6, pc}
Tree.TreeNode.delete.L0: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r1, [r1, #12]                        /* Load field from struct */
    mov r0, r5
    add lr, pc, #8
    mov pc, r4
    mov r6, r0
    mvn r1, #0
    cmp r6, r1
    bne Tree.TreeNode.delete.L3
    push { r5 }
    push { r4 }
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldmfd sp!, {r1, r2}
    bl Tree.TreeNode.delete_P_1              /* Call Tree.TreeNode.delete */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r0, [r1, #4]                         /* Store value to struct field */
    b Tree.TreeNode.delete.L2
Tree.TreeNode.delete.L3: 
    cmp r6, #1
    bne Tree.TreeNode.delete.L4
    push { r5 }
    push { r4 }
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    ldmfd sp!, {r1, r2}
    bl Tree.TreeNode.delete_P_1              /* Call Tree.TreeNode.delete */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r0, [r1, #8]                         /* Store value to struct field */
    b Tree.TreeNode.delete.L2
Tree.TreeNode.delete.L4: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldr r1, .POOL@-1914853343_1_L1_NULL      /* Load null address */
    cmp r0, r1
    bne Tree.TreeNode.delete.L7
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    ldmfd sp!, {r3-r6, pc}
Tree.TreeNode.delete.L7: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    ldr r1, .POOL@-1914853343_1_L1_NULL      /* Load null address */
    cmp r0, r1
    bne Tree.TreeNode.delete.L8
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldmfd sp!, {r3-r6, pc}
Tree.TreeNode.delete.L8: 
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r5, [r1, #8]                         /* Load field from struct */
Tree.TreeNode.delete.L11:                    /* Evaluate condition */
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r0, [r1, #4]                         /* Load field from struct */
    ldr r1, .POOL@-1914853343_1_L1_NULL      /* Load null address */
    cmp r0, r1
    beq Tree.TreeNode.delete.L12
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r5, [r1, #4]                         /* Load field from struct */
    b Tree.TreeNode.delete.L11
Tree.TreeNode.delete.L12: 
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r0, [r1, #12]                        /* Load field from struct */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r0, [r1, #12]                        /* Store value to struct field */
    lsl r1, r5, #2                           /* Convert to bytes */
    ldr r0, [r1, #12]                        /* Load field from struct */
    push { r0 }
    push { r4 }
    lsl r1, r3, #2                           /* Convert to bytes */
    ldr r0, [r1, #8]                         /* Load field from struct */
    ldmfd sp!, {r1, r2}
    bl Tree.TreeNode.delete_P_1              /* Call Tree.TreeNode.delete */
    lsl r1, r3, #2                           /* Convert to bytes */
    str r0, [r1, #8]                         /* Store value to struct field */
Tree.TreeNode.delete.L2: 
    mov r0, r3                               /* Evaluate Expression */
    ldmfd sp!, {r3-r6, pc}
.POOL@-1914853343_1_L1_NULL: .word NULL
    
.global Tree.TreeNode.create_P_1
Tree.TreeNode.create_P_1:                    /* Function: Tree.TreeNode.create, Provisos: INT */
    push { r3, lr }
    mov r3, r0
    push { r3 }
    ldr r0, .POOL@-1914853343_2_L1_NULL      /* Load null address */
    ldr r1, .POOL@-1914853343_2_L1_NULL      /* Load null address */
    stmfd sp!, {r0, r1}
    ldr r0, .POOL@-1914853343_2_Tree.TreeNode
    push { r0 }
    bl init_P_4                              /* Call init */
    ldmfd sp!, {r3, pc}
.POOL@-1914853343_2_Tree.TreeNode: .word Tree.TreeNode
.POOL@-1914853343_2_L1_NULL: .word NULL
