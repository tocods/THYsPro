package gpu.allocation;

import gpu.Vgpu;
import gpu.VideoCard;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public class VideoCardAllocationPolicyNull extends VideoCardAllocationPolicy {

	public VideoCardAllocationPolicyNull(List<? extends VideoCard> videoCards) {
		super(videoCards);
	}

	@Override
	public boolean allocate(Vgpu vgpu, int PCIeBw) {
		throw new NotImplementedException("");
	}

	@Override
	public boolean allocate(VideoCard videoCard, Vgpu vgpu, int PCIeBw) {
		throw new NotImplementedException("");
	}

}
