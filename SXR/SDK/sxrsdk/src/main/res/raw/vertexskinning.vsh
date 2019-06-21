#if defined(HAS_a_bone_indices) && defined(HAS_a_bone_weights)
	mat4 bone = u_bone_matrix[a_bone_indices[0] + 1] * a_bone_weights[0];
	bone += u_bone_matrix[a_bone_indices[1] + 1] * a_bone_weights[1];
	bone += u_bone_matrix[a_bone_indices[2] + 1] * a_bone_weights[2];
	bone += u_bone_matrix[a_bone_indices[3] + 1] * a_bone_weights[3];
	vertex.local_position = bone * vertex.local_position;
	vertex.local_position = u_bone_matrix[0] * vertex.local_position;
#endif