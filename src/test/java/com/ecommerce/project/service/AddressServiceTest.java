package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private AddressServiceImpl addressService;

    private Address address;
    private AddressDTO addressDTO;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUserId(1L);
        user.setUserName("testuser");
        user.setEmail("test@example.com");
        user.setAddresses(new ArrayList<>());

        address = new Address();
        address.setAddressId(1L);
        address.setStreet("123 Main St");
        address.setBuildingName("Tower A");
        address.setCity("New York");
        address.setState("NY");
        address.setCountry("USA");
        address.setPincode("10001");
        address.setUser(user);

        addressDTO = new AddressDTO();
        addressDTO.setAddressId(1L);
        addressDTO.setStreet("123 Main St");
        addressDTO.setBuildingName("Tower A");
        addressDTO.setCity("New York");
        addressDTO.setState("NY");
        addressDTO.setCountry("USA");
        addressDTO.setPincode("10001");
    }

    @Nested
    @DisplayName("createAddress() Tests")
    class CreateAddressTests {

        @Test
        @DisplayName("Should create address and associate with user")
        void createAddress_Success() {
            when(modelMapper.map(addressDTO, Address.class)).thenReturn(address);
            when(addressRepository.save(any(Address.class))).thenReturn(address);
            when(modelMapper.map(address, AddressDTO.class)).thenReturn(addressDTO);

            AddressDTO result = addressService.createAddress(addressDTO, user);

            assertNotNull(result);
            assertEquals("123 Main St", result.getStreet());
            assertEquals("New York", result.getCity());
            verify(addressRepository).save(any(Address.class));
        }

        @Test
        @DisplayName("Should add address to user's address list")
        void createAddress_AddsToUserAddressList() {
            when(modelMapper.map(addressDTO, Address.class)).thenReturn(address);
            when(addressRepository.save(any(Address.class))).thenReturn(address);
            when(modelMapper.map(address, AddressDTO.class)).thenReturn(addressDTO);

            addressService.createAddress(addressDTO, user);

            assertTrue(user.getAddresses().contains(address));
        }
    }

    @Nested
    @DisplayName("getAddresses() Tests")
    class GetAllAddressesTests {

        @Test
        @DisplayName("Should return all addresses")
        void getAddresses_Success() {
            List<Address> addresses = List.of(address);
            when(addressRepository.findAll()).thenReturn(addresses);
            when(modelMapper.map(any(Address.class), eq(AddressDTO.class))).thenReturn(addressDTO);

            List<AddressDTO> result = addressService.getAddresses();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("123 Main St", result.get(0).getStreet());
        }

        @Test
        @DisplayName("Should return empty list when no addresses exist")
        void getAddresses_EmptyList() {
            when(addressRepository.findAll()).thenReturn(Collections.emptyList());

            List<AddressDTO> result = addressService.getAddresses();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("getAddressesById() Tests")
    class GetAddressByIdTests {

        @Test
        @DisplayName("Should return address by ID")
        void getAddressesById_Success() {
            when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
            when(modelMapper.map(address, AddressDTO.class)).thenReturn(addressDTO);

            AddressDTO result = addressService.getAddressesById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getAddressId());
            assertEquals("New York", result.getCity());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for invalid ID")
        void getAddressesById_NotFound_ThrowsException() {
            when(addressRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> addressService.getAddressesById(999L)
            );
        }
    }

    @Nested
    @DisplayName("getUserAddresses() Tests")
    class GetUserAddressesTests {

        @Test
        @DisplayName("Should return all addresses for a user")
        void getUserAddresses_Success() {
            user.setAddresses(List.of(address));
            when(modelMapper.map(any(Address.class), eq(AddressDTO.class))).thenReturn(addressDTO);

            List<AddressDTO> result = addressService.getUserAddresses(user);

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("Should return empty list when user has no addresses")
        void getUserAddresses_NoAddresses_ReturnsEmptyList() {
            user.setAddresses(Collections.emptyList());

            List<AddressDTO> result = addressService.getUserAddresses(user);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("updateAddress() Tests")
    class UpdateAddressTests {

        @Test
        @DisplayName("Should update address successfully")
        void updateAddress_Success() {
            AddressDTO updatedDTO = new AddressDTO();
            updatedDTO.setStreet("456 New St");
            updatedDTO.setCity("Los Angeles");
            updatedDTO.setState("CA");
            updatedDTO.setCountry("USA");
            updatedDTO.setPincode("90001");
            updatedDTO.setBuildingName("Building B");

            Address updatedAddress = new Address();
            updatedAddress.setAddressId(1L);
            updatedAddress.setStreet("456 New St");
            updatedAddress.setCity("Los Angeles");
            updatedAddress.setUser(user);

            user.setAddresses(new ArrayList<>(List.of(address)));

            when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
            when(addressRepository.save(any(Address.class))).thenReturn(updatedAddress);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(modelMapper.map(any(Address.class), eq(AddressDTO.class))).thenReturn(updatedDTO);

            AddressDTO result = addressService.updateAddress(1L, updatedDTO);

            assertNotNull(result);
            assertEquals("456 New St", result.getStreet());
            assertEquals("Los Angeles", result.getCity());
            verify(addressRepository).save(any(Address.class));
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for invalid address ID")
        void updateAddress_NotFound_ThrowsException() {
            when(addressRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> addressService.updateAddress(999L, addressDTO)
            );
        }
    }

    @Nested
    @DisplayName("deleteAddress() Tests")
    class DeleteAddressTests {

        @Test
        @DisplayName("Should delete address successfully")
        void deleteAddress_Success() {
            user.setAddresses(new ArrayList<>(List.of(address)));
            when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
            when(userRepository.save(any(User.class))).thenReturn(user);

            String result = addressService.deleteAddress(1L);

            assertNotNull(result);
            assertTrue(result.contains("deleted successfully"));
            verify(addressRepository).delete(address);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("Should remove address from user's address list when deleted")
        void deleteAddress_RemovesFromUserList() {
            user.setAddresses(new ArrayList<>(List.of(address)));
            when(addressRepository.findById(1L)).thenReturn(Optional.of(address));
            when(userRepository.save(any(User.class))).thenReturn(user);

            addressService.deleteAddress(1L);

            assertFalse(user.getAddresses().stream()
                .anyMatch(a -> a.getAddressId().equals(1L)));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException for invalid address ID")
        void deleteAddress_NotFound_ThrowsException() {
            when(addressRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                () -> addressService.deleteAddress(999L)
            );

            verify(addressRepository, never()).delete(any());
        }
    }
}
