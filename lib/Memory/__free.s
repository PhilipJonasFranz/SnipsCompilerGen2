free:
	push {r1, r2, r3}
	ldr r3, [r0]
	rsb r3, r3, #0
	
	add r1, r3, r0
free_loop:
	ldr r2, [r1]
	cmp r2, #0
	bge free_end
	add r3, r3, r2
	sub r1, r1, r2
	b free_loop
free_end:
	str r3, [r0]
	pop {r1, r2, r3}
	bx lr