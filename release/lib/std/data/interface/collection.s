.include iterable.s
    
.text
    
    add r10, r12, #4
    mov r12, #0
    add pc, pc, r10
    add r10, r10, r10                        /* Function was not called, use as placeholder */
    b List.LinkedList.get_P_1
    add r10, r10, r10                        /* Function was not called, use as placeholder */
    b List.LinkedList.add_P_1
    add r10, r10, r10                        /* Function was not called, use as placeholder */
    
